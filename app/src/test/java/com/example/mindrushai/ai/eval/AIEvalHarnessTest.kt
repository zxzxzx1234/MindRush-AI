package com.example.mindrushai.ai.eval

import com.example.mindrushai.ai.AIManager
import com.example.mindrushai.ai.SequenceGeneratorAI
import com.example.mindrushai.ai.llm.FakeLLMClient
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * AI evaluation harness — runs each agent against a corpus of mocked LLM responses
 * (clean, noisy, partially malformed) and verifies it meets the thresholds documented in
 * docs/TESTING.md section 4.1.
 *
 * Thresholds (from TESTING.md):
 *   SequenceGeneratorAI:
 *     - Output parseability        ≥ 95%
 *     - Length match               ≥ 90%
 *     - Valid tokens               ≥ 95%
 *     - No consecutive duplicates  ≥ 80%
 *
 *   DifficultyAdjusterAI (via AIManager.adjustDifficulty):
 *     - Output format valid        100%
 *     - Monotonicity (good → up,
 *       bad → down)                ≥ 85%
 *     - Stability (mid → keep)     ≥ 80%
 */
class AIEvalHarnessTest {

    private val evalRuns = 100
    private val rng = Random(42)

    // ---------- SequenceGeneratorAI evals ----------

    /**
     * Mix of well-formed, noisy, and partially malformed model outputs.
     * Even with junk responses, AIManager must always return a usable sequence.
     */
    private fun noisyResponder(length: Int): (String) -> String = {
        when (rng.nextInt(0, 6)) {
            0 -> (1..length).joinToString(",") { rng.nextInt(0, 4).toString() }
            1 -> "[${(1..length).joinToString(" ") { rng.nextInt(0, 4).toString() }}]"
            2 -> "0,1,2,3,9,42," + (1..length).joinToString(",") { rng.nextInt(0, 4).toString() }
            3 -> "I think it should be 0 then 1 then 2"
            4 -> "{\"sequence\": [0,1,2,3]}"
            else -> ""
        }
    }

    @Test
    fun `sequence generator eval - parseability and length`() = runTest {
        val length = 6
        val mgr = AIManager(FakeLLMClient(responder = noisyResponder(length)))

        var parseable = 0
        var lengthMatch = 0
        var validTokens = 0
        var noConsecutiveDup = 0

        repeat(evalRuns) {
            val out = mgr.generateSequence(length = length, difficulty = 5)

            if (out.isNotEmpty()) parseable++
            if (out.size == length) lengthMatch++
            if (out.all { it in 0..3 }) validTokens++
            if (!hasConsecutiveDuplicates(out)) noConsecutiveDup++
        }

        assertThreshold("parseability", parseable, evalRuns, 0.95)
        assertThreshold("length match", lengthMatch, evalRuns, 0.90)
        assertThreshold("valid tokens", validTokens, evalRuns, 0.95)
        assertThreshold("no consecutive duplicates", noConsecutiveDup, evalRuns, 0.80)
    }

    @Test
    fun `local sequence generator alone passes all quality thresholds`() {
        val gen = SequenceGeneratorAI()
        val length = 8

        var lengthMatch = 0
        var validTokens = 0
        var noConsecutiveDup = 0

        repeat(evalRuns) {
            val out = gen.generateSequence(length, difficulty = 6)
            if (out.size == length) lengthMatch++
            if (out.all { it in 0..3 }) validTokens++
            if (!hasConsecutiveDuplicates(out)) noConsecutiveDup++
        }

        assertThreshold("local length match", lengthMatch, evalRuns, 0.95)
        assertThreshold("local valid tokens", validTokens, evalRuns, 1.0)
        assertThreshold("local no consecutive duplicates", noConsecutiveDup, evalRuns, 0.95)
    }

    // ---------- DifficultyAdjusterAI evals ----------

    @Test
    fun `difficulty adjuster eval - output format always valid`() = runTest {
        val mgr = AIManager(llmClient = null)

        var validFormat = 0
        repeat(evalRuns) {
            val rate = rng.nextFloat()
            val time = 200.0 + rng.nextDouble() * 4000.0
            val current = rng.nextInt(1, 11)
            val out = mgr.adjustDifficulty(current, rate, time)
            if (out in 1..10) validFormat++
        }

        assertThreshold("difficulty format", validFormat, evalRuns, 1.0)
    }

    @Test
    fun `difficulty adjuster eval - monotonicity good versus bad performance`() = runTest {
        val mgr = AIManager(llmClient = null)

        var goodIncreasesOrCaps = 0
        var badDecreasesOrFloors = 0

        repeat(evalRuns) {
            val current = rng.nextInt(2, 9)

            // High performance
            val good = mgr.adjustDifficulty(current, 0.9f + rng.nextFloat() * 0.1f, 600.0)
            if (good >= current) goodIncreasesOrCaps++

            // Bad performance
            val bad = mgr.adjustDifficulty(current, rng.nextFloat() * 0.3f, 3500.0)
            if (bad <= current) badDecreasesOrFloors++
        }

        assertThreshold("good → up/cap", goodIncreasesOrCaps, evalRuns, 0.85)
        assertThreshold("bad → down/floor", badDecreasesOrFloors, evalRuns, 0.85)
    }

    @Test
    fun `difficulty adjuster eval - stability in mid zone`() = runTest {
        val mgr = AIManager(llmClient = null)

        var stable = 0
        repeat(evalRuns) {
            val current = rng.nextInt(2, 9)
            // Mid zone: success ~ 0.5–0.7, time ~ 1300–2200ms
            val rate = 0.5f + rng.nextFloat() * 0.2f
            val time = 1300.0 + rng.nextDouble() * 900.0
            val out = mgr.adjustDifficulty(current, rate, time)
            if (out == current) stable++
        }

        assertThreshold("mid-zone stability", stable, evalRuns, 0.80)
    }

    // ---------- helpers ----------

    private fun hasConsecutiveDuplicates(seq: List<Int>): Boolean {
        for (i in 1 until seq.size) {
            if (seq[i] == seq[i - 1]) return true
        }
        return false
    }

    private fun assertThreshold(
        label: String,
        passed: Int,
        total: Int,
        threshold: Double
    ) {
        val ratio = passed.toDouble() / total
        val pct = "%.2f%%".format(ratio * 100)
        val target = "%.2f%%".format(threshold * 100)
        assertTrue(
            "$label: $passed/$total ($pct) below threshold $target",
            ratio >= threshold
        )
    }
}
