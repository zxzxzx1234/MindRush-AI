package com.example.mindrushai.ai.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * LLM backend targeting a locally-running LM Studio instance.
 *
 * The Android emulator reaches the host machine via the special alias 10.0.2.2.
 * LM Studio must expose its OpenAI-compatible endpoint on port 1234:
 *   LM Studio → Local Server tab → Start Server
 *
 * Timeout rationale:
 *   connect  2 s  — fast on loopback; failure means LM Studio is not running
 *   read    10 s  — small CPU models can take 5–8 s for hint/validation prompts
 *   write    3 s  — prompt payloads are small
 *
 * This client never throws — all exceptions are caught and "" is returned
 * so upstream agents can activate their fallback logic transparently.
 */
class LMStudioClient(
    private val baseUrl    : String = "http://10.0.2.2:1234",
    private val modelId    : String = "local-model",
    private val temperature: Double = 0.7
) : LLMClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(2,  TimeUnit.SECONDS)
        .readTimeout(10,    TimeUnit.SECONDS)
        .writeTimeout(3,    TimeUnit.SECONDS)
        .build()

    override suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("model",       modelId)
                put("temperature", temperature)
                put("max_tokens",  150)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role",    "user")
                        put("content", prompt)
                    })
                })
            }

            val request = Request.Builder()
                .url("$baseUrl/v1/chat/completions")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext ""
                val body = response.body?.string() ?: return@withContext ""
                JSONObject(body)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim()
            }
        } catch (_: Exception) {
            ""   // safe fallback — agents handle empty string gracefully
        }
    }
}