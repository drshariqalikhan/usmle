package com.step2ck.scanquiz.auth

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Handles OpenRouter Bring-Your-Own-Key (BYOK) authentication via OAuth 2.0 PKCE.
 * Ensures API keys are strictly kept on-device inside Android Keystore.
 */
class OpenRouterAuthManager(private val context: Context) {

    private val client = OkHttpClient()

    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val securePrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "openrouter_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private const val PREF_API_KEY = "openrouter_api_key"
        private const val PREF_CODE_VERIFIER = "pkce_code_verifier"
        const val REDIRECT_URI = "step2ck://oauth-callback"
        const val AUTH_URL = "https://openrouter.ai/auth"
        const val TOKEN_EXCHANGE_URL = "https://openrouter.ai/api/v1/auth/keys"
    }

    /**
     * Generates a high-entropy PKCE Code Verifier (64 URL-safe characters)
     */
    fun generateCodeVerifier(): String {
        val bytes = ByteArray(48)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    /**
     * Generates SHA-256 Code Challenge from the verifier
     */
    fun generateCodeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(hash, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    /**
     * Builds the OpenRouter OAuth authorization URL for browser or Chrome Custom Tab
     */
    fun createAuthorizationUri(): Uri {
        val verifier = generateCodeVerifier()
        securePrefs.edit().putString(PREF_CODE_VERIFIER, verifier).apply()

        val challenge = generateCodeChallenge(verifier)
        return Uri.parse(AUTH_URL).buildUpon()
            .appendQueryParameter("callback_url", REDIRECT_URI)
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .build()
    }

    /**
     * Exchanges auth code received in deep link for user's OpenRouter API key
     */
    suspend fun exchangeCodeForApiKey(authCode: String): Result<String> = withContext(Dispatchers.IO) {
        val codeVerifier = securePrefs.getString(PREF_CODE_VERIFIER, null)
            ?: return@withContext Result.failure(IllegalStateException("No PKCE code verifier found in secure storage"))

        try {
            val jsonPayload = JSONObject().apply {
                put("code", authCode)
                put("code_verifier", codeVerifier)
                put("code_challenge_method", "S256")
            }

            val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(TOKEN_EXCHANGE_URL)
                .post(body)
                .header("HTTP-Referer", "https://step2ck-companion.local")
                .header("X-Title", "Step 2 CK Scan-to-Quiz")
                .build()

            val response = client.newCall(request).execute()
            val respBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("OAuth exchange failed HTTP ${response.code}: $respBody"))
            }

            val json = JSONObject(respBody)
            val apiKey = json.optString("key", "")
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("Response did not contain an API key"))
            }

            // Save key in Android Keystore EncryptedSharedPreferences
            saveApiKey(apiKey)
            // Clear used verifier
            securePrefs.edit().remove(PREF_CODE_VERIFIER).apply()

            Result.success(apiKey)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun saveApiKey(apiKey: String) {
        securePrefs.edit().putString(PREF_API_KEY, apiKey.trim()).apply()
    }

    fun getApiKey(): String? {
        return securePrefs.getString(PREF_API_KEY, null)
    }

    fun isConnected(): Boolean {
        val key = getApiKey()
        return !key.isNullOrBlank()
    }

    fun clearAuth() {
        securePrefs.edit().clear().apply()
    }
}

