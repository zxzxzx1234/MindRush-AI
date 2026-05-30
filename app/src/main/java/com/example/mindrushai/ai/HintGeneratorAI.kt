package com.example.mindrushai.ai

import com.example.mindrushai.ai.llm.LLMClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * AI Agent 3 — HintGeneratorAI
 *
 * Generates a short, contextually meaningful hint for a word the player
 * failed to recall. Shown on the Game Over screen.
 *
 * Why this cannot be done deterministically:
 *   Useful memory hints require semantic understanding, cultural knowledge,
 *   and creative variation. The same word in the same position can yield
 *   a different but equally valid hint each call. A lookup table would be
 *   enormous, brittle, and produce repetitive output.
 *
 * Context-awareness:
 *   The prompt includes the full round sequence, the word's position within
 *   it, and its neighbours. Memory research shows associative cues
 *   ("it came after CANDLE and before ROCKET") are more effective than
 *   isolated definitions.
 *
 * Progressive explicitness:
 *   Attempt 1 → subtle, evocative
 *   Attempt 2 → moderately direct
 *   Attempt 3+ → reveals first letter and word length
 */
class HintGeneratorAI(
    private val llmClient: LLMClient? = null
) {

    companion object {
        private const val LLM_TIMEOUT_MS = 5000L
    }

    /**
     * @param word          The word the player failed to recall.
     * @param sequence      The full word sequence for this round.
     * @param wordIndex     Position of [word] within [sequence] (0-based).
     * @param attemptNumber How many times the player has failed (starts at 1).
     */
    suspend fun generateHint(
        word: String,
        sequence: List<String> = emptyList(),
        wordIndex: Int = 0,
        attemptNumber: Int = 1
    ): String {
        return if (llmClient != null) {
            tryLLMHint(word, sequence, wordIndex, attemptNumber)
        } else {
            localHint(word, attemptNumber)
        }
    }

    // ── LLM path ──────────────────────────────────────────────────────────────

    private suspend fun tryLLMHint(
        word: String,
        sequence: List<String>,
        wordIndex: Int,
        attemptNumber: Int
    ): String {
        return try {
            val raw = withContext(Dispatchers.IO) {
                withTimeoutOrNull(LLM_TIMEOUT_MS) {
                    llmClient!!.generate(buildPrompt(word, sequence, wordIndex, attemptNumber))
                }
            }

            if (raw.isNullOrBlank()) {
                return localHint(word, attemptNumber).also {
                    AILogger.log("HINT_TIMEOUT", "word=$word attempt=$attemptNumber", it)
                }
            }

            val hint = raw.trim().removePrefix("\"").removeSuffix("\"").trim()
            AILogger.log("HINT_LLM", "word=$word pos=$wordIndex attempt=$attemptNumber", hint)
            hint

        } catch (e: Exception) {
            localHint(word, attemptNumber).also {
                AILogger.log("HINT_EXCEPTION", "word=$word err=${e.message}", it)
            }
        }
    }

    // ── Prompt ────────────────────────────────────────────────────────────────

    private fun buildPrompt(
        word: String,
        sequence: List<String>,
        wordIndex: Int,
        attemptNumber: Int
    ): String {

        val positionContext = buildString {
            if (sequence.size > 1) {
                appendLine("Round context: the full sequence was [${sequence.joinToString(", ")}].")
                appendLine("The forgotten word was #${wordIndex + 1} of ${sequence.size}.")
                val before = if (wordIndex > 0) sequence[wordIndex - 1] else null
                val after  = if (wordIndex < sequence.lastIndex) sequence[wordIndex + 1] else null
                when {
                    before != null && after != null ->
                        appendLine("It appeared between \"$before\" and \"$after\".")
                    before != null ->
                        appendLine("It was the last word, after \"$before\".")
                    after != null ->
                        appendLine("It was the first word, before \"$after\".")
                }
            }
        }

        val style = when (attemptNumber) {
            1    -> "Subtle and evocative — evoke meaning through imagery, do not reveal it directly."
            2    -> "Moderately explicit — give a clearer semantic clue."
            else -> "Explicit — mention the first letter '${word.first()}' and that it has ${word.length} letters."
        }

        return """
A player in a memory word game failed to recall a specific word. The word was "$word".

$positionContext
Write ONE concise hint (maximum 12 words) to help them remember it.
Style: $style
Rules:
- Do NOT use the word itself, its plural, or any direct derivative
- Do NOT reference other words from the sequence in the hint
- Be creative — vivid, unexpected hints are more memorable than definitions

Output ONLY the hint. No quotes, labels, or explanation.
        """.trimIndent()
    }

    // ── Local fallback ────────────────────────────────────────────────────────

    private fun localHint(word: String, attemptNumber: Int): String {
        val hint = when {
            attemptNumber >= 3 -> "Starts with '${word.first()}' — ${word.length} letters total."
            attemptNumber == 2 -> "A ${word.length}-letter word starting with '${word.first()}'."
            word.length <= 4   -> "A short, everyday English word."
            word.length <= 7   -> "A common word of medium length."
            else               -> "A longer, less common English word."
        }
        AILogger.log("HINT_LOCAL", "word=$word attempt=$attemptNumber", hint)
        return hint
    }
}