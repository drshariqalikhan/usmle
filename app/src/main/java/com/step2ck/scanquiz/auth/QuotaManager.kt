package com.step2ck.scanquiz.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Model representing the user's OpenRouter free-tier quota status.
 */
data class QuotaInfo(
    val isValid: Boolean,
    val label: String,
    val limit: Double?,
    val usage: Double,
    val isFreeTier: Boolean,
    val rateLimitRequestsPerMin: Int = 20,
    val dailyLimitRequests: Int = 50,
    val dailyRemaining: Int = 50
)

/**
 * Manages quota pre-flight checks and rate-limit guardrails.
 * OpenRouter free accounts are subject to:
 * - 50 requests/day (resets 00:00 UTC)
 * - 20 requests/minute
 * - 5-second cooldown between quiz generations
 */
class QuotaManager(
    private val authManager: OpenRouterAuthManager,
    private val client: OkHttpClient = OkHttpClient()
) {

    private var lastRequestTimestamp: Long = 0
    private val cooldownDurationMs: Long = 5000L // 5s button cooldown

    fun canInitiateQuizRequest(): Pair<Boolean, Long> {
        val now = System.currentTimeMillis()
        val elapsed = now - lastRequestTimestamp
        return if (elapsed < cooldownDurationMs) {
            val waitRemainingSec = ((cooldownDurationMs - elapsed) / 1000) + 1
            Pair(false, waitRemainingSec)
        } else {
            Pair(true, 0)
        }
    }

    fun recordQuizRequestMade() {
        lastRequestTimestamp = System.currentTimeMillis()
    }

    /**
     * Calls GET /api/v1/key before scanning.
     * Displays remaining daily free requests without consuming quota.
     */
    suspend fun checkQuota(): Result<QuotaInfo> = withContext(Dispatchers.IO) {
        val apiKey = authManager.getApiKey()
            ?: return@withContext Result.failure(IllegalStateException("No OpenRouter API key found. Connect OpenRouter first."))

        try {
            val request = Request.Builder()
                .url("https://openrouter.ai/api/v1/key")
                .header("Authorization", "Bearer $apiKey")
                .header("HTTP-Referer", "https://step2ck-companion.local")
                .header("X-Title", "Step 2 CK Scan-to-Quiz")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val respBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Quota check failed HTTP ${response.code}: $respBody"))
            }

            val root = JSONObject(respBody)
            val data = root.optJSONObject("data")
                ?: return@withContext Result.failure(Exception("Malformed quota response: missing 'data'"))

            val label = data.optString("label", "Connected Key")
            val limit = if (data.isNull("limit")) null else data.optDouble("limit", 0.0)
            val usage = data.optDouble("usage", 0.0)
            val isFreeTier = data.optBoolean("is_free_tier", true)
            
            val rateLimitObj = data.optJSONObject("rate_limit")
            val requestsPerMin = rateLimitObj?.optInt("requests", 20) ?: 20

            // Estimate daily remaining based on standard 50 reqs/day
            val dailyRemaining = if (isFreeTier) 50 else 999

            Result.success(
                QuotaInfo(
                    isValid = true,
                    label = label,
                    limit = limit,
                    usage = usage,
                    isFreeTier = isFreeTier,
                    rateLimitRequestsPerMin = requestsPerMin,
                    dailyRemaining = dailyRemaining
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

