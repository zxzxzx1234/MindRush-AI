package com.example.mindrushai.ai.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * LLM backend targeting a locally-running Ollama instance.
 *
 * Default model: llama3. Override [modelId] to use any model
 * available in your local Ollama installation.
 *
 * Uses the /api/generate endpoint (non-streaming).
 */
class OllamaClient(
    private val baseUrl: String = "http://10.0.2.2:11434",
    private val modelId: String = "llama3"
) : LLMClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(2,  TimeUnit.SECONDS)
        .readTimeout(10,    TimeUnit.SECONDS)
        .writeTimeout(3,    TimeUnit.SECONDS)
        .build()

    override suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("model",  modelId)
                put("prompt", prompt)
                put("stream", false)
            }

            val request = Request.Builder()
                .url("$baseUrl/api/generate")
                .post(payload.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext ""
                val body = response.body?.string() ?: return@withContext ""
                JSONObject(body).optString("response", "").trim()
            }
        } catch (_: Exception) {
            ""
        }
    }
}