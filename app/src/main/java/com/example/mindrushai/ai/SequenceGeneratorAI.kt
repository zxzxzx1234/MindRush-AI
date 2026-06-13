package com.example.mindrushai.ai

import com.example.mindrushai.ai.llm.LLMClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * AI Agent 1 — SequenceGeneratorAI
 *
 * Generates English words for each round, calibrated to difficulty.
 *
 * Word length progression (directly tied to difficulty level):
 *   Difficulty 1  → 3-letter words  (cat, run, joy)
 *   Difficulty 2  → 4-letter words  (book, lamp, tree)
 *   Difficulty 3  → 5-letter words  (apple, cloud, brave)
 *   Difficulty 4  → 5-letter words  (stone, flame, crisp)
 *   Difficulty 5  → 6-letter words  (bridge, planet, silver)
 *   Difficulty 6  → 6-letter words  (rocket, jungle, candle)
 *   Difficulty 7  → 7-letter words  (blanket, journey, crystal)
 *   Difficulty 8  → 7-letter words  (lantern, thunder, mystery)
 *   Difficulty 9  → 8+ letter words (labyrinth, cognition)
 *   Difficulty 10 → 8+ letter words (ephemeral, paradigm)
 *
 * Prompt design: extremely minimal — local LLMs hallucinate less and respond
 * faster with short, unambiguous prompts.
 *
 * Timeout: 15s — small CPU models can take 10–12s for a list of words.
 *
 * Fallback: curated static pools, shuffled every call.
 */
class SequenceGeneratorAI(
    private val llmClient: LLMClient? = null
) {

    companion object {
        private const val LLM_TIMEOUT_MS = 15_000L
    }

    // ── Fallback pools ────────────────────────────────────────────────────────

    private val words3 = listOf(
        "cat","dog","run","joy","sun","map","key","cup","fly","pen",
        "arm","sky","red","ant","bus","gem","hop","ice","jar","log",
        "mud","net","owl","pod","ski","web","bay","elm","fur","gut"
    )

    private val words4 = listOf(
        "book","lamp","tree","boat","cake","door","fire","gold","hand","iron",
        "jump","kind","lake","moon","nose","open","park","rain","salt","time",
        "wave","year","zero","blue","calm","dark","edge","farm","gate","hill"
    )

    private val words5 = listOf(
        "apple","cloud","brave","stone","flame","crisp","brand","chess","drift","eagle",
        "flair","grace","hover","ivory","joker","kneel","lemon","maple","noble","ozone",
        "pixel","quiet","risen","scout","thorn","unity","vapor","whirl","xenon","yacht"
    )

    private val words6 = listOf(
        "bridge","planet","silver","rocket","jungle","candle","castle","desert","engine",
        "glider","harbor","island","magnet","needle","quartz","riddle","temple","velvet",
        "anchor","cactus","goblet","hammer","mirror","pocket","sunset","forest","market",
        "rubber","tunnel","window"
    )

    private val words7 = listOf(
        "blanket","journey","crystal","lantern","thunder","mystery","cabinet","climate",
        "dolphin","eclipse","fantasy","granite","harmony","iceberg","justice","kingdom",
        "leopard","monitor","nervous","organic","paradox","quantum","railway","silence",
        "torpedo","upgrade","villain","whisper","extreme","fragile"
    )

    private val words8plus = listOf(
        "labyrinth","cognition","resonance","melancholy","symposium","eloquence",
        "paradigm","serendipity","tenacious","juxtapose","soliloquy","ephemeral",
        "clandestine","oscillate","ubiquitous","ineffable","sycophant","recondite",
        "palimpsest","perfidious","loquacious","equivocate","byzantine","inscrutable",
        "sanguine","truculent","mellifluous","obfuscate","querulous","perihelion"
    )

    // ── Public API ────────────────────────────────────────────────────────────

    suspend fun generateSequence(length: Int, difficulty: Int): List<String> {
        if (length <= 0) return emptyList()

        return if (llmClient != null) {
            tryLLMGenerate(length, difficulty)
        } else {
            fallback(length, difficulty).also {
                AILogger.log("SEQUENCE_NO_CLIENT", "len=$length d=$difficulty", it.joinToString(","))
            }
        }
    }

    fun localFallback(length: Int, difficulty: Int) = fallback(length, difficulty)

    // ── LLM path ──────────────────────────────────────────────────────────────

    private suspend fun tryLLMGenerate(length: Int, difficulty: Int): List<String> {
        return try {
            val prompt = buildPrompt(length, difficulty)

            AILogger.log("SEQUENCE_REQUESTING", "len=$length d=$difficulty", prompt.take(120))

            val raw = withContext(Dispatchers.IO) {
                withTimeoutOrNull(LLM_TIMEOUT_MS) {
                    llmClient!!.generate(prompt)
                }
            }

            AILogger.log("SEQUENCE_RAW_RESPONSE", "len=$length d=$difficulty", raw ?: "NULL/TIMEOUT")

            if (raw.isNullOrBlank()) {
                return fallback(length, difficulty).also {
                    AILogger.log("SEQUENCE_TIMEOUT_FALLBACK", "len=$length d=$difficulty", it.joinToString(","))
                }
            }

            val parsed = parseWords(raw, length, difficulty)
            AILogger.log("SEQUENCE_LLM_SUCCESS", "len=$length d=$difficulty", parsed.joinToString(","))
            parsed

        } catch (e: Exception) {
            fallback(length, difficulty).also {
                AILogger.log("SEQUENCE_EXCEPTION", e.message ?: "?", it.joinToString(","))
            }
        }
    }

    // ── Prompt — kept minimal on purpose ─────────────────────────────────────

    private fun buildPrompt(length: Int, difficulty: Int): String {
        val (letterCount, description) = wordProfile(difficulty)

        return """
List $length different $description English words.
Output ONLY the words separated by commas. Nothing else.
Example: word,word,word
        """.trimIndent()
    }

    private fun wordProfile(difficulty: Int): Pair<String, String> = when (difficulty) {
        1    -> "3-letter" to "3-letter common"
        2    -> "4-letter" to "4-letter common"
        3, 4 -> "5-letter" to "5-letter common"
        5, 6 -> "6-letter" to "6-letter"
        7, 8 -> "7-letter" to "7-letter"
        else -> "8+ letter" to "long and uncommon"
    }

    // ── Parser — handles messy LLM output ────────────────────────────────────

    private fun parseWords(raw: String, expectedLength: Int, difficulty: Int): List<String> {
        // Strip common LLM preamble patterns like "Here are 5 words:" or "1. cat"
        val cleaned = raw
            .replace(Regex("(?i)(here are|sure|output|words?:?|list:?)"), "")
            .replace(Regex("\\d+[.)\\-]\\s*"), " ")  // remove numbering "1. " "2) "
            .replace(Regex("[\\[\\]\"'()\\n;:.!?]"), ",")
            .replace(Regex(",+"), ",")
            .trim(',', ' ')

        val words = cleaned
            .split(",")
            .map { it.trim().lowercase().filter { c -> c.isLetter() } }
            .filter { it.length >= 2 }
            .distinct()

        AILogger.log("SEQUENCE_PARSED", "got ${words.size} of $expectedLength", words.joinToString(","))

        return if (words.size >= expectedLength) {
            words.take(expectedLength)
        } else {
            // supplement with fallback, avoiding duplicates
            val supplement = fallback(expectedLength * 2, difficulty).filter { it !in words }
            (words + supplement).distinct().take(expectedLength)
        }
    }

    // ── Fallback pools ────────────────────────────────────────────────────────

    private fun fallback(length: Int, difficulty: Int): List<String> {
        val pool = when (difficulty) {
            1    -> words3
            2    -> words4
            3, 4 -> words5
            5, 6 -> words6
            7, 8 -> words7
            else -> words8plus
        }
        return pool.shuffled().take(length.coerceAtMost(pool.size))
    }
}