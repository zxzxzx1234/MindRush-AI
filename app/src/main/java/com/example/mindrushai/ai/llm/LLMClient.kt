package com.example.mindrushai.ai.llm

/**
 * Common contract for all LLM backends.
 * Implementations must run I/O on a background dispatcher and
 * return "" on any error — never throw to callers.
 */
interface LLMClient {
    suspend fun generate(prompt: String): String
}