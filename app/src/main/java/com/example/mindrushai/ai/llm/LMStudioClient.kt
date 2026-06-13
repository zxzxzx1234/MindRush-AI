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
 * REQUIRED in AndroidManifest.xml:
 *   <uses-permission android:name="android.permission.INTERNET" />
 *   android:usesCleartextTraffic="true"   ← inside <application>
 *
 * Emulator → host via 10.0.2.2:1234
 * Physical device → PC's LAN IP (e.g. 192.168.1.x:1234)
 */
class LMStudioClient(
    private val baseUrl    : String = "http://10.0.2.2:1234",
    private val modelId    : String = "local-model",
    private val temperature: Double = 0.7
) : LLMClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(3,  TimeUnit.SECONDS)
        .readTimeout(20,    TimeUnit.SECONDS)   // generous — slow CPU models need time
        .writeTimeout(5,    TimeUnit.SECONDS)
        .build()

    override suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("model",       modelId)
                put("temperature", temperature)
                put("max_tokens",  200)
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
                if (!response.isSuccessful) {
                    android.util.Log.e("MindRushAI", "LMStudio HTTP error: ${response.code}")
                    return@withContext ""
                }
                val body = response.body?.string() ?: return@withContext ""
                JSONObject(body)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim()
            }
        } catch (e: Exception) {
            android.util.Log.e("MindRushAI", "LMStudio exception: ${e.message}")
            ""
        }
    }
}