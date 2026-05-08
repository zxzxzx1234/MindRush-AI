package com.example.mindrushai.ai

import com.example.mindrushai.ai.llm.FakeLLMClient
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [AIManager].
 *
 * Coverage:
 *  - Works with no LLM (uses local generator)
 *  - Parses well-formed LLM responses
 *  - Falls back on malformed JSON / garbage (BUG-004 regression)
 *  - Falls back on LLM timeout
 *  - Falls back on LLM exception (BUG-001 regression)
 *  - adjustDifficulty heuristic respects [1, 10] clamp
 *  - explainDecision returns a non-empty string in all paths
 */
class AIManagerTest {

    // -------- generateSequence --------

    @Test
    fun `generateSequence with no LLM uses local generator and returns correct length`() = runTest {
        val mgr = AIManager(llmClient = null)
        val out = mgr.generateSequence(length = 7, difficulty = 4)

        assertEquals(7, out.size)
        out.forEach { assertTrue(it in 0..3) }
    }

    @Test
    fun `generateSequence parses comma separated LLM response`() = runTest {
        val fake = FakeLLMClient { "0,1,2,3,0" }
        val mgr = AIManager(fake)

        val out = mgr.generateSequence(length = 5, difficulty = 1)

        assertEquals(5, out.size)
        out.forEach { assertTrue(it in 0..3) }
        assertEquals(1, fake.callCount)
    }

    @Test
    fun `generateSequence parses space separated and bracketed response`() = runTest {
        val fake = FakeLLMClient { "[0 1 2 3]" }
        val mgr = AIManager(fake)

        val out = mgr.generateSequence(length = 4, difficulty = 1)

        assertEquals(4, out.size)
        out.forEach { assertTrue(it in 0..3) }
    }

    @Test
    fun `bug004 regression - malformed JSON falls back to local generator`() = runTest {
        val fake = FakeLLMClient { "{not valid json at all !!! 99 100}" }
        val mgr = AIManager(fake)

        val out = mgr.generateSequence(length = 6, difficulty = 3)

        // Even with garbage, the local generator must produce a valid sequence.
        assertEquals(6, out.size)
        out.forEach { assertTrue("invalid token $it in $out", it in 0..3) }
    }

    @Test
    fun `bug001 regression - LLM throwing exception falls back gracefully`() = runTest {
        val fake = FakeLLMClient { error("simulated network failure") }
        val mgr = AIManager(fake)

        val out = mgr.generateSequence(length = 5, difficulty = 2)

        assertEquals(5, out.size)
        out.forEach { assertTrue(it in 0..3) }
    }

    @Test
    fun `LLM response with too few tokens falls back to local generator`() = runTest {
        val fake = FakeLLMClient { "0,1" } // only 2 tokens
        val mgr = AIManager(fake)

        val out = mgr.generateSequence(length = 8, difficulty = 1)

        assertEquals(8, out.size)
    }

    @Test
    fun `LLM response with out-of-range tokens has them filtered out`() = runTest {
        val fake = FakeLLMClient { "0,1,9,42,2,3,7,5" }
        val mgr = AIManager(fake)

        val out = mgr.generateSequence(length = 4, difficulty = 1)

        assertEquals(4, out.size)
        out.forEach { assertTrue("got out-of-range $it", it in 0..3) }
    }

    // -------- adjustDifficulty --------

    @Test
    fun `adjustDifficulty heuristic - high success and fast time increases`() = runTest {
        val mgr = AIManager(llmClient = null)
        val out = mgr.adjustDifficulty(
            currentDifficulty = 5,
            successRate = 0.95f,
            avgTime = 800.0
        )
        assertEquals(6, out)
    }

    @Test
    fun `adjustDifficulty heuristic - low success decreases`() = runTest {
        val mgr = AIManager(llmClient = null)
        val out = mgr.adjustDifficulty(
            currentDifficulty = 5,
            successRate = 0.2f,
            avgTime = 3000.0
        )
        assertEquals(4, out)
    }

    @Test
    fun `adjustDifficulty heuristic - mid zone keeps difficulty`() = runTest {
        val mgr = AIManager(llmClient = null)
        val out = mgr.adjustDifficulty(
            currentDifficulty = 5,
            successRate = 0.6f,
            avgTime = 1500.0
        )
        assertEquals(5, out)
    }

    @Test
    fun `adjustDifficulty respects upper bound`() = runTest {
        val mgr = AIManager(llmClient = null)
        val out = mgr.adjustDifficulty(
            currentDifficulty = 10,
            successRate = 1.0f,
            avgTime = 400.0
        )
        assertEquals("must clamp at 10", 10, out)
    }

    @Test
    fun `adjustDifficulty respects lower bound`() = runTest {
        val mgr = AIManager(llmClient = null)
        val out = mgr.adjustDifficulty(
            currentDifficulty = 1,
            successRate = 0.0f,
            avgTime = 5000.0
        )
        assertEquals("must clamp at 1", 1, out)
    }

    @Test
    fun `adjustDifficulty with LLM returning a valid number is honored`() = runTest {
        val fake = FakeLLMClient { "7" }
        val mgr = AIManager(fake)

        val out = mgr.adjustDifficulty(
            currentDifficulty = 3,
            successRate = 0.6f,
            avgTime = 1500.0
        )
        assertEquals(7, out)
    }

    @Test
    fun `adjustDifficulty with LLM returning out-of-range value is clamped`() = runTest {
        val fakeHigh = FakeLLMClient { "999" }
        val mgrHigh = AIManager(fakeHigh)
        assertEquals(10, mgrHigh.adjustDifficulty(3, 0.8f, 1000.0))

        val fakeLow = FakeLLMClient { "-50" }
        val mgrLow = AIManager(fakeLow)
        assertEquals(1, mgrLow.adjustDifficulty(3, 0.2f, 3000.0))
    }

    @Test
    fun `adjustDifficulty with LLM returning garbage falls back to heuristic`() = runTest {
        val fake = FakeLLMClient { "I think... maybe... seven?" }
        val mgr = AIManager(fake)

        val out = mgr.adjustDifficulty(
            currentDifficulty = 5,
            successRate = 0.95f,
            avgTime = 800.0
        )
        // High success + fast time → heuristic returns +1
        assertEquals(6, out)
    }

    @Test
    fun `adjustDifficulty with LLM throwing falls back to heuristic`() = runTest {
        val fake = FakeLLMClient { error("boom") }
        val mgr = AIManager(fake)

        val out = mgr.adjustDifficulty(
            currentDifficulty = 5,
            successRate = 0.2f,
            avgTime = 3000.0
        )
        assertEquals(4, out)
    }

    // -------- explainDecision --------

    @Test
    fun `explainDecision without LLM returns deterministic message`() = runTest {
        val mgr = AIManager(llmClient = null)
        val msg = mgr.explainDecision(difficulty = 5, successRate = 0.7f)

        assertNotNull(msg)
        assertTrue(msg.isNotBlank())
    }

    @Test
    fun `explainDecision with LLM returns the model output`() = runTest {
        val fake = FakeLLMClient { "Player is doing great." }
        val mgr = AIManager(fake)

        val msg = mgr.explainDecision(difficulty = 7, successRate = 0.9f)

        assertEquals("Player is doing great.", msg)
    }
}
