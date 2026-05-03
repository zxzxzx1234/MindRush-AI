package com.example.mindrushai.ai.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class LMStudioClient : LLMClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .writeTimeout(3, TimeUnit.SECONDS)
        .build()

    override suspend fun generate(prompt: String): String =
        withContext(Dispatchers.IO) {

            try {

                val json = JSONObject().apply {
                    put("model", "local-model")
                    put("temperature", 0.7)
                    put("max_tokens", 50)

                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        })
                    })
                }

                val body = json.toString()
                    .toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("http://10.0.2.2:1234/v1/chat/completions")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->

                    if (!response.isSuccessful) {
                        throw Exception("HTTP error: ${response.code}")
                    }

                    val responseBody = response.body?.string()
                        ?: throw Exception("Empty response")

                    val jsonResponse = JSONObject(responseBody)

                    val content = jsonResponse
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")

                    content.trim()
                }

            } catch (e: Exception) {

                // fallback safe → IMPORTANT
                ""
            }
        }
}