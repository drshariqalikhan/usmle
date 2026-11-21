package com.step2ck.scanquiz.llm

import com.step2ck.scanquiz.auth.OpenRouterAuthManager
import com.step2ck.scanquiz.auth.QuotaManager
import com.step2ck.scanquiz.scanner.AggregatedScanSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * OpenRouter Client adhering strictly to the architecture constraints:
 * - Exactly ONE API call per quiz session.
 * - JSON mode enforcement (response_format = { "type": "json_object" }).
 * - Locked free model routing array.
 * - Local rate-limit and 429 backoff handling.
 */
class OpenRouterClient(
    private val authManager: OpenRouterAuthManager,
    private val quotaManager: QuotaManager
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val API_URL = "https://openrouter.ai/api/v1/chat/completions"

        // Locked fallback chain for free tier models
        val FREE_MODELS_FALLBACK_CHAIN = listOf(
            "meta-llama/llama-3.3-70b-instruct:free",
            "google/gemma-3-27b-it:free",
            "qwen/qwen-2.5-72b-instruct:free",
            "openrouter/free"
        )
    }

    /**
     * Executes the SINGLE OpenRouter API call to generate the quiz.
     */
    suspend fun generateQuiz(
        summary: AggregatedScanSummary,
        includeTables: Boolean,
        includeFigures: Boolean,
        questionCount: Int = 5
    ): Result<ValidatedQuiz> = withContext(Dispatchers.IO) {
        // 1. Quota & cooldown check
        val (canRequest, waitSec) = quotaManager.canInitiateQuizRequest()
        if (!canRequest) {
            return@withContext Result.failure(
                IllegalStateException("Please wait $waitSec seconds before generating another quiz (rate limit protection).")
            )
        }

        val apiKey = authManager.getApiKey()
            ?: return@withContext Result.failure(IllegalStateException("OpenRouter API key is missing. Please connect OpenRouter."))

        // 2. Assemble single prompt payload
        val userContent = PromptAssembler.buildUserMessage(summary, includeTables, includeFigures, questionCount)

        // Try primary model, with max 1 fallback retry on 429 or provider congestion
        var lastException: Exception? = null

        for ((attemptIndex, modelName) in FREE_MODELS_FALLBACK_CHAIN.withIndex()) {
            try {
                quotaManager.recordQuizRequestMade()

                val messagesArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", PromptAssembler.SYSTEM_PROMPT)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userContent)
                    })
                }

                val payload = JSONObject().apply {
                    put("model", modelName)
                    put("messages", messagesArray)
                    put("response_format", JSONObject().apply {
                        put("type", "json_object")
                    })
                    put("temperature", 0.2)
                }

                val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(API_URL)
                    .post(requestBody)
                    .header("Authorization", "Bearer $apiKey")
                    .header("HTTP-Referer", "https://step2ck-companion.local")
                    .header("X-Title", "Step 2 CK Scan-to-Quiz")
                    .build()

                val response = client.newCall(request).execute()
                val statusCode = response.code
                val responseBodyStr = response.body?.string() ?: ""

                // Handle 429 Too Many Requests
                if (statusCode == 429) {
                    if (responseBodyStr.contains("openrouter_free_tier_daily")) {
                        return@withContext Result.failure(
                            Exception("OpenRouter daily free-tier limit reached (50 requests/day). Resets at 00:00 UTC.")
                        )
                    }
                    // Provider congestion: wait 5s and try fallback model
                    if (attemptIndex < FREE_MODELS_FALLBACK_CHAIN.size - 1) {
                        delay(5000)
                        continue
                    }
                }

                if (!response.isSuccessful) {
                    throw Exception("OpenRouter request failed HTTP $statusCode: $responseBodyStr")
                }

                val jsonResponse = JSONObject(responseBodyStr)
                val choices = jsonResponse.optJSONArray("choices")
                    ?: throw Exception("Invalid OpenRouter response: missing 'choices'")
                if (choices.length() == 0) throw Exception("Empty choices in response")

                val messageObj = choices.getJSONObject(0).optJSONObject("message")
                    ?: throw Exception("Missing 'message' in response choice")
                val content = messageObj.optString("content", "")

                val validationResult = QuizJsonValidator.validateAndParse(content)
                if (validationResult.isSuccess) {
                    return@withContext validationResult
                } else {
                    lastException = Exception("Schema validation error: ${validationResult.exceptionOrNull()?.message}")
                    // Retry with fallback model if schema was truncated or broken
                    if (attemptIndex < 2) continue
                }
            } catch (e: Exception) {
                lastException = e
                if (attemptIndex >= 1) break // Max 1 retry
            }
        }

        Result.failure(lastException ?: Exception("Quiz generation failed across all free model endpoints."))
    }
}

