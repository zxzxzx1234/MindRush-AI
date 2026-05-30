package com.example.mindrushai.ai.llm

/**
 * Common contract for all LLM backends (LM Studio, Ollama, etc.)
 *
 * Implementations must:
 *   - Execute network I/O on a background dispatcher (e.g. [kotlinx.coroutines.Dispatchers.IO])
 *   - Return an empty string on any error — never throw to callers
 *   - Respect the HTTP timeouts configured at the client level
 */
interface LLMClient {
    /**
     * Sends [prompt] to the model and returns the raw text response.
     * Returns an empty string if the model is unreachable or returns no content.
     */
    suspend fun generate(prompt: String): String
}