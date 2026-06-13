package com.example.mindrushai.ai

import com.example.mindrushai.ai.llm.LLMClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * AI Agent 3 — HintGeneratorAI
 *
 * Generates a short contextual hint for a word the player failed to recall.
 * Prompt is kept minimal to work reliably with small local models.
 * Timeout: 12s.
 */
class HintGeneratorAI(
    private val llmClient: LLMClient? = null
) {

    companion object {
        private const val LLM_TIMEOUT_MS = 12_000L
    }

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

    private suspend fun tryLLMHint(
        word: String,
        sequence: List<String>,
        wordIndex: Int,
        attemptNumber: Int
    ): String {
        return try {
            val prompt = buildPrompt(word, sequence, wordIndex, attemptNumber)

            AILogger.log("HINT_REQUESTING", "word=$word attempt=$attemptNumber", "")

            val raw = withContext(Dispatchers.IO) {
                withTimeoutOrNull(LLM_TIMEOUT_MS) {
                    llmClient!!.generate(prompt)
                }
            }

            AILogger.log("HINT_RAW_RESPONSE", "word=$word", raw ?: "NULL/TIMEOUT")

            if (raw.isNullOrBlank()) {
                return localHint(word, attemptNumber).also {
                    AILogger.log("HINT_TIMEOUT_FALLBACK", word, it)
                }
            }

            val hint = raw.trim()
                .removePrefix("\"").removeSuffix("\"")
                .lines().first()   // take only the first line if model outputs multiple
                .trim()

            AILogger.log("HINT_LLM_SUCCESS", "word=$word pos=$wordIndex", hint)
            hint

        } catch (e: Exception) {
            localHint(word, attemptNumber).also {
                AILogger.log("HINT_EXCEPTION", "word=$word err=${e.message}", it)
            }
        }
    }

    private fun buildPrompt(
        word: String,
        sequence: List<String>,
        wordIndex: Int,
        attemptNumber: Int
    ): String {
        val position = if (sequence.size > 1) {
            val before = if (wordIndex > 0) sequence[wordIndex - 1] else null
            val after  = if (wordIndex < sequence.lastIndex) sequence[wordIndex + 1] else null
            when {
                before != null && after != null -> " It came after \"$before\" and before \"$after\"."
                before != null                  -> " It was the last word, after \"$before\"."
                after  != null                  -> " It was the first word, before \"$after\"."
                else                            -> ""
            }
        } else ""

        val explicitness = when (attemptNumber) {
            1    -> "Be subtle, do not reveal the word."
            2    -> "Be a bit more direct."
            else -> "Give the first letter '${word.first()}' and mention it has ${word.length} letters."
        }

        return """
Give a hint for the English word "$word".$position
One sentence, max 10 words. Do not use the word itself. $explicitness
Output only the hint.
        """.trimIndent()
    }

    private fun localHint(word: String, attemptNumber: Int): String {
        return when {
            attemptNumber >= 3 -> "Starts with '${word.first()}' — ${word.length} letters."
            attemptNumber == 2 -> "${word.length} letters, starts with '${word.first()}'."
            word.length <= 4   -> "A short, common English word."
            word.length <= 7   -> "A medium-length English word."
            else               -> "A longer, less common word."
        }.also {
            AILogger.log("HINT_LOCAL", "word=$word attempt=$attemptNumber", it)
        }
    }
}