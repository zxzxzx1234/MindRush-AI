package com.example.mindrushai.ai.llm

import okhttp3.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class OllamaClient : LLMClient {

    private val client = OkHttpClient()

    override suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        val json = JSONObject()
        json.put("model", "llama3")
        json.put("prompt", prompt)
        json.put("stream", false)

        val body = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url("http://10.0.2.2:11434/api/generate")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        JSONObject(responseBody).optString("response", "0")
    }
}