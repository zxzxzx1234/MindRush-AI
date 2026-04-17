package com.example.mindrushai.ai.llm

import okhttp3.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class OpenAIClient(private val apiKey: String) : LLMClient {

    private val client = OkHttpClient()

    override suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {

        val json = JSONObject()
        json.put("model", "gpt-4o-mini")

        val messages = JSONArray()
        val msg = JSONObject()
        msg.put("role", "user")
        msg.put("content", prompt)

        messages.put(msg)
        json.put("messages", messages)

        val body = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        val jsonResponse = JSONObject(responseBody)
        jsonResponse
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
    }
}