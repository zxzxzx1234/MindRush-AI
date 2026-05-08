package com.example.mindrushai.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [SequenceGeneratorAI].
 *
 * Coverage:
 *  - generateSequence respects requested length
 *  - All values fall in the valid token range [0, 3]
 *  - refineSequence removes consecutive duplicates (BUG-003 regression)
 *  - generateNext returns a valid token for every difficulty bucket
 *  - Empty / zero-length inputs are handled gracefully
 *  - Diversity check: high-difficulty sequences are not pathologically repetitive
 */
class SequenceGeneratorAITest {

    private val gen = SequenceGeneratorAI()

    // -------- Length / range --------

    @Test
    fun `generateSequence with length 0 returns empty list`() {
        val out = gen.generateSequence(length = 0, difficulty = 1)
        assertTrue("expected empty list, got $out", out.isEmpty())
    }

    @Test
    fun `generateSequence with negative length returns empty list`() {
        val out = gen.generateSequence(length = -3, difficulty = 5)
        assertTrue(out.isEmpty())
    }

    @Test
    fun `generateSequence returns exactly the requested length`() {
        for (len in 1..12) {
            val out = gen.generateSequence(length = len, difficulty = 5)
            assertEquals("length mismatch for len=$len", len, out.size)
        }
    }

    @Test
    fun `every value is in 0 to 3`() {
        for (difficulty in 1..10) {
            val out = gen.generateSequence(length = 20, difficulty = difficulty)
            out.forEach { v ->
                assertTrue(
                    "invalid token $v at difficulty=$difficulty in $out",
                    v in 0..3
                )
            }
        }
    }

    // -------- BUG-003 regression: no consecutive duplicates after refine --------

    @Test
    fun `bug003 refineSequence removes consecutive duplicates`() {
        val raw = listOf(0, 0, 1, 1, 1, 2, 3, 3)
        val refined = gen.refineSequence(raw, difficulty = 5)

        assertEquals(raw.size, refined.size)
        for (i in 1 until refined.size) {
            assertTrue(
                "consecutive duplicate at index $i in $refined",
                refined[i] != refined[i - 1]
            )
        }
        refined.forEach { assertTrue(it in 0..3) }
    }

    @Test
    fun `refineSequence on empty input returns empty list`() {
        val out = gen.refineSequence(emptyList(), difficulty = 3)
        assertTrue(out.isEmpty())
    }

    @Test
    fun `refineSequence on single element preserves it`() {
        val out = gen.refineSequence(listOf(2), difficulty = 1)
        assertEquals(listOf(2), out)
    }

    // -------- generateNext smoke tests across difficulty buckets --------

    @Test
    fun `generateNext returns valid token for empty sequence at any difficulty`() {
        for (difficulty in 1..10) {
            val v = gen.generateNext(emptyList(), difficulty)
            assertTrue("got $v for difficulty=$difficulty", v in 0..3)
        }
    }

    @Test
    fun `generateNext returns valid token for non-empty sequence`() {
        val seed = listOf(0, 1, 2, 3, 0, 1)
        for (difficulty in 1..10) {
            val v = gen.generateNext(seed, difficulty)
            assertNotNull(v)
            assertTrue("got $v for difficulty=$difficulty", v in 0..3)
        }
    }

    // -------- Diversity / quality heuristic --------

    @Test
    fun `high difficulty long sequence covers more than one token`() {
        // Statistical smoke check: with length=20, the chance of producing a
        // single-token sequence by accident is negligible. If we ever observe it,
        // something has gone wrong with the generator.
        val out = gen.generateSequence(length = 20, difficulty = 8)
        val unique = out.toSet().size
        assertTrue(
            "expected diverse tokens at high difficulty, got $out (unique=$unique)",
            unique >= 2
        )
    }

    @Test
    fun `refined sequence preserves length`() {
        val raw = listOf(1, 1, 1, 1, 1, 1, 1, 1)
        val refined = gen.refineSequence(raw, difficulty = 7)
        assertEquals(raw.size, refined.size)
    }
}
