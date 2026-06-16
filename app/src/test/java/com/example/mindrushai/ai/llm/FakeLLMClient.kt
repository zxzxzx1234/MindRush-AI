package com.example.mindrushai.ai

import com.example.mindrushai.ai.llm.LLMClient

/**
 * Test double for [LLMClient] that returns a fixed response.
 * Tracks how many times [generate] was called for cache-hit verification.
 */
class FakeLLMClient(private val response: String) : LLMClient {
    var callCount: Int = 0
        private set

    override suspend fun generate(prompt: String): String {
        callCount++
        return response
    }
}

/**
 * Test double for [LLMClient] that always throws an exception.
 * Used to verify fallback behaviour on LLM failure.
 */
class ThrowingLLMClient : LLMClient {
    override suspend fun generate(prompt: String): String {
        throw RuntimeException("Simulated LLM network failure")
    }
}