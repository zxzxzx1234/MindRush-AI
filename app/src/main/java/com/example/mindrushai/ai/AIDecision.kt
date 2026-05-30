package com.example.mindrushai.ai

/**
 * Immutable record of a single AI decision, used for logging and debugging.
 *
 * @param type      Machine-readable event tag  (e.g. "SEQUENCE_LLM", "WORD_VALID_LLM")
 * @param input     The context or prompt that triggered this decision.
 * @param output    The result produced by the AI agent.
 * @param timestamp Unix epoch ms, set at creation time.
 */
data class AIDecision(
    val type: String,
    val input: String,
    val output: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun of(type: String, input: String, output: String) =
            AIDecision(type = type, input = input, output = output)
    }
}