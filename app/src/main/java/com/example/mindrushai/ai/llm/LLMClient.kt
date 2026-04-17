package com.example.mindrushai.ai.llm

interface LLMClient {
    suspend fun generate(prompt: String): String
}