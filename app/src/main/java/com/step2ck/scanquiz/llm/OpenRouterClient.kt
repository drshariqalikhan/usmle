package com.step2ck.scanquiz.llm

import com.step2ck.scanquiz.auth.OpenRouterAuthManager
import com.step2ck.scanquiz.auth.QuotaManager
import com.step2ck.scanquiz.debug.AppLogger
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
 * - Exactly ONE successful API call per quiz session.
 * - Robust fallback chain across high-quality free models.
 * - Automatic retry without response_format if provider rejects json_object.
 * - Comprehensive event & error logging to AppLogger.
 */
class OpenRouterClient(
    private val authManager: OpenRouterAuthManager,
    private val quotaManager: QuotaManager
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val API_URL = "https://openrouter.ai/api/v1/chat/completions"

        // Reliable fallback chain for OpenRouter free tier models
        val DEFAULT_FREE_MODELS = listOf(
            "meta-llama/llama-3.3-70b-instruct:free",
            "google/gemini-2.0-flash-exp:free",
            "qwen/qwen-2.5-72b-instruct:free",
            "deepseek/deepseek-chat:free",
            "mistralai/mistral-small-24b-instruct-2501:free",
            "openrouter/free"
        )
    }

    /**
     * Executes the OpenRouter API call to generate the quiz.
     */
    suspend fun generateQuiz(
        summary: AggregatedScanSummary,
        includeTables: Boolean,
        includeFigures: Boolean,
        questionCount: Int = 5
    ): Result<ValidatedQuiz> = withContext(Dispatchers.IO) {
        AppLogger.d("LLM", "Starting quiz generation. Pages: ${summary.totalPages}, Tables: $includeTables, Figures: $includeFigures, Qs: $questionCount")

        // 1. Quota & cooldown check
        val (canRequest, waitSec) = quotaManager.canInitiateQuizRequest()
        if (!canRequest) {
            val msg = "Please wait $waitSec seconds before generating another quiz (rate limit protection)."
            AppLogger.d("LLM", msg)
            return@withContext Result.failure(IllegalStateException(msg))
        }

        val apiKey = authManager.getApiKey()
        if (apiKey.isNullOrBlank()) {
            val msg = "OpenRouter API key is missing. Please connect or paste your key in Settings."
            AppLogger.e("LLM", msg)
            return@withContext Result.failure(IllegalStateException(msg))
        }

        // 2. Determine model sequence (custom model first if configured)
        val customModel = authManager.getCustomModel()
        val modelsToTry = if (!customModel.isNullOrBlank()) {
            listOf(customModel) + DEFAULT_FREE_MODELS.filter { it != customModel }
        } else {
            DEFAULT_FREE_MODELS
        }

        // 3. Assemble prompt payload
        val userContent = PromptAssembler.buildUserMessage(summary, includeTables, includeFigures, questionCount)
        AppLogger.d("LLM", "User prompt assembled (${userContent.length} chars).")

        var lastException: Exception? = null

        for ((attemptIndex, modelName) in modelsToTry.withIndex()) {
            try {
                AppLogger.d("LLM", "Attempting [${attemptIndex + 1}/${modelsToTry.size}] with model: $modelName")
                quotaManager.recordQuizRequestMade()

                // Try with json_object mode first, fallback to standard if rejected
                var response = executeCompletionRequest(modelName, apiKey, userContent, enforceJsonObject = true)
                var statusCode = response.first
                var responseBodyStr = response.second

                // If provider rejected json_object response_format, immediately retry without it
                if (statusCode == 400 && (responseBodyStr.contains("response_format") || responseBodyStr.contains("json_object"))) {
                    AppLogger.d("LLM", "[$modelName] Provider does not support response_format json_object. Retrying without response_format...")
                    response = executeCompletionRequest(modelName, apiKey, userContent, enforceJsonObject = false)
                    statusCode = response.first
                    responseBodyStr = response.second
                }

                // Handle 429 Too Many Requests
                if (statusCode == 429) {
                    AppLogger.e("LLM", "[$modelName] 429 Rate Limited: $responseBodyStr")
                    if (responseBodyStr.contains("openrouter_free_tier_daily")) {
                        return@withContext Result.failure(
                            Exception("OpenRouter daily free-tier limit reached (50 requests/day). Resets at 00:00 UTC.")
                        )
                    }
                    if (attemptIndex < modelsToTry.size - 1) {
                        delay(3000)
                        continue
                    }
                }

                if (statusCode !in 200..299) {
                    val errMsg = "HTTP $statusCode from $modelName: $responseBodyStr"
                    AppLogger.e("LLM", errMsg)
                    lastException = Exception(errMsg)
                    continue
                }

                AppLogger.d("LLM", "[$modelName] HTTP $statusCode OK! Received ${responseBodyStr.length} chars.")

                val jsonResponse = JSONObject(responseBodyStr)
                val choices = jsonResponse.optJSONArray("choices")
                    ?: throw Exception("Invalid OpenRouter response: missing 'choices'")
                if (choices.length() == 0) throw Exception("Empty 'choices' array in response")

                val messageObj = choices.getJSONObject(0).optJSONObject("message")
                    ?: throw Exception("Missing 'message' in response choice")
                val content = messageObj.optString("content", "")

                AppLogger.d("LLM", "Validating JSON output (${content.length} chars)...")
                val validationResult = QuizJsonValidator.validateAndParse(content)

                if (validationResult.isSuccess) {
                    val quiz = validationResult.getOrThrow()
                    AppLogger.d("LLM", "SUCCESS: Validated ${quiz.questions.size} questions for '${quiz.quizTitle}'!")
                    return@withContext validationResult
                } else {
                    val valErr = validationResult.exceptionOrNull()?.message ?: "Unknown validation error"
                    AppLogger.e("LLM", "[$modelName] Schema validation error: $valErr")
                    lastException = Exception("[$modelName] Schema validation error: $valErr")
                    continue
                }
            } catch (e: Exception) {
                AppLogger.e("LLM", "Exception during model $modelName", e)
                lastException = e
            }
        }

        val finalError = lastException ?: Exception("Quiz generation failed across all fallback models.")
        AppLogger.e("LLM", "All attempts exhausted: ${finalError.message}")
        Result.failure(finalError)
    }

    private fun executeCompletionRequest(
        modelName: String,
        apiKey: String,
        userContent: String,
        enforceJsonObject: Boolean
    ): Pair<Int, String> {
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
            if (enforceJsonObject) {
                put("response_format", JSONObject().apply {
                    put("type", "json_object")
                })
            }
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
        return Pair(statusCode, responseBodyStr)
    }
}
