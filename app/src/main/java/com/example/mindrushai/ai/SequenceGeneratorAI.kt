package com.example.mindrushai.ai

import com.example.mindrushai.ai.llm.LLMClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * AI Agent 1 — SequenceGeneratorAI
 *
 * Generates English words for each round, calibrated to the current difficulty.
 * Called once per round — always produces a fresh set of words via the LLM.
 *
 * Why a language model instead of a static list?
 *   The LLM is non-deterministic: the same prompt yields different words every
 *   call. It also optimises for memorability — selecting words with phonetic
 *   contrast and semantic variety — something a fixed list cannot do.
 *
 * Word profiles by difficulty:
 *   1–3  → short (3–4 letters), high-frequency   e.g. cat, run, joy
 *   4–6  → medium (5–7 letters), everyday         e.g. blanket, forest
 *   7–10 → long (8+ letters), rare/advanced       e.g. ephemeral, labyrinth
 *
 * Fallback: curated static pools used only when the LLM is unavailable.
 *   Pools are shuffled on every call so output is never predictable.
 */
class SequenceGeneratorAI(
    private val llmClient: LLMClient? = null
) {

    companion object {
        private const val LLM_TIMEOUT_MS = 6000L
    }

    // ── Fallback pools (manually curated for phonetic contrast) ───────────────

    private val easyWords = listOf(
        "cat", "run", "joy", "sun", "map", "key", "cup", "fly",
        "pen", "arm", "sky", "red", "ant", "bus", "gem", "hop",
        "ice", "jar", "log", "mud", "net", "owl", "pod", "ski",
        "web", "bay", "elm", "fur", "gut", "ivy", "box", "hat"
    )

    private val mediumWords = listOf(
        "blanket", "journey", "pillow", "rocket", "ladder", "candle",
        "silver", "tunnel", "sunset", "forest", "planet", "castle",
        "desert", "engine", "glider", "harbor", "island", "jungle",
        "magnet", "needle", "quartz", "riddle", "temple", "velvet",
        "anchor", "cactus", "goblet", "hammer", "mirror", "pocket"
    )

    private val hardWords = listOf(
        "ephemeral", "labyrinth", "cognition", "resonance", "melancholy",
        "symposium", "oscillate", "ubiquitous", "clandestine", "eloquence",
        "paradigm", "serendipity", "tenacious", "juxtapose", "soliloquy",
        "querulous", "recondite", "palimpsest", "ineffable", "sycophant",
        "perfidious", "obfuscate", "perihelion", "loquacious", "mellifluous",
        "equivocate", "byzantine", "inscrutable", "sanguine", "truculent"
    )

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns [length] words appropriate for [difficulty].
     * Tries the LLM first; falls back to static pools on failure.
     * Always returns exactly [length] items.
     */
    suspend fun generateSequence(length: Int, difficulty: Int): List<String> {
        if (length <= 0) return emptyList()

        return if (llmClient != null) {
            tryLLMGenerate(length, difficulty)
        } else {
            localFallback(length, difficulty).also {
                AILogger.log("SEQUENCE_NO_CLIENT", "len=$length d=$difficulty", it.joinToString(","))
            }
        }
    }

    // ── LLM path ──────────────────────────────────────────────────────────────

    private suspend fun tryLLMGenerate(length: Int, difficulty: Int): List<String> {
        return try {
            val raw = withContext(Dispatchers.IO) {
                withTimeoutOrNull(LLM_TIMEOUT_MS) {
                    llmClient!!.generate(buildPrompt(length, difficulty))
                }
            }

            if (raw.isNullOrBlank()) {
                return localFallback(length, difficulty).also {
                    AILogger.log("SEQUENCE_TIMEOUT", "len=$length d=$difficulty", it.joinToString(","))
                }
            }

            val words = parseWords(raw, length, difficulty)
            AILogger.log("SEQUENCE_LLM", "len=$length d=$difficulty", words.joinToString(","))
            words

        } catch (e: Exception) {
            localFallback(length, difficulty).also {
                AILogger.log("SEQUENCE_EXCEPTION", e.message ?: "unknown", it.joinToString(","))
            }
        }
    }

    // ── Prompt ────────────────────────────────────────────────────────────────

    private fun buildPrompt(length: Int, difficulty: Int): String {
        val profile = when (difficulty) {
            in 1..3 -> "very short (3–4 letters) and extremely common English words"
            in 4..6 -> "medium-length (5–7 letters) and moderately common English words"
            else    -> "long (8+ letters) and rare or advanced English vocabulary words"
        }

        val memoryGuidance = when (difficulty) {
            in 1..3 -> """
Memory optimisation:
- Words must sound CLEARLY DIFFERENT (avoid rhymes like cat/bat/hat)
- Words must LOOK DIFFERENT on screen (avoid similar shapes like clam/clan)
- Each word should belong to a DIFFERENT category (not two animals, not two colours)"""
            in 4..6 -> """
Memory optimisation:
- Avoid words from the same semantic field (not two nature words, not two household items)
- Vary word length across the list (mix 5-letter and 7-letter words)
- Prefer words with distinct consonant patterns"""
            else    -> """
Memory optimisation:
- Select words from completely unrelated semantic domains
- Avoid words sharing the same prefix or suffix
- Prefer words with vivid, concrete imagery — they are harder to confuse"""
        }

        return """
You are selecting words for a memory game. The player sees each word briefly, then must recall them all in order.

Generate exactly $length $profile.
$memoryGuidance

Hard rules:
- Every word must exist in a standard English dictionary
- No proper nouns, abbreviations, or hyphenated words
- All lowercase, no duplicates

Respond with ONLY a comma-separated list. No numbering, no explanation.
Example: word1,word2,word3
        """.trimIndent()
    }

    // ── Parsing ───────────────────────────────────────────────────────────────

    private fun parseWords(raw: String, expectedLength: Int, difficulty: Int): List<String> {
        val words = raw
            .replace(Regex("[\\[\\]\"'()\\n.;:\\d]"), ",")
            .split(",")
            .map { it.trim().lowercase().filter { c -> c.isLetter() } }
            .filter { it.length >= 2 }
            .distinct()

        return if (words.size >= expectedLength) {
            words.take(expectedLength)
        } else {
            val supplement = localFallback(expectedLength - words.size, difficulty)
                .filter { it !in words }
            (words + supplement).take(expectedLength)
        }
    }

    // ── Local fallback ────────────────────────────────────────────────────────

    fun localFallback(length: Int, difficulty: Int): List<String> {
        val pool = when (difficulty) {
            in 1..3 -> easyWords
            in 4..6 -> mediumWords
            else    -> hardWords
        }
        return pool.shuffled().take(length.coerceAtMost(pool.size))
    }
}