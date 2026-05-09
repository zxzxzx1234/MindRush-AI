package com.example.mindrushai.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [DifficultyAdjusterAI].
 *
 * Coverage:
 *  - Initial state and reset
 *  - Difficulty stays clamped in [1, 10]
 *  - Increases when player performs well (high success + fast time)
 *  - Decreases when player struggles (low success / slow time)
 *  - Stable in mid-zone (avoids flapping)
 *  - Guard against acting on too-small history
 *  - Regression for BUG-002 (no NaN with empty history)
 */
class DifficultyAdjusterAITest {

    private fun feed(
        adjuster: DifficultyAdjusterAI,
        rounds: Int,
        success: Boolean,
        responseTime: Long
    ) {
        repeat(rounds) {
            adjuster.update(success, responseTime)
        }
    }

    // -------- Initial state --------

    @Test
    fun `initial difficulty is 1`() {
        val adjuster = DifficultyAdjusterAI()
        assertEquals(1, adjuster.difficulty)
    }

    @Test
    fun `reset returns difficulty to 1 and clears history`() {
        val adjuster = DifficultyAdjusterAI()
        feed(adjuster, rounds = 8, success = true, responseTime = 500L)
        assertTrue(
            "expected difficulty to climb above 1 after 8 perfect rounds",
            adjuster.difficulty > 1
        )

        adjuster.reset()

        assertEquals(1, adjuster.difficulty)
    }

    // -------- Increase / decrease behavior --------

    @Test
    fun `high success and fast response time increases difficulty`() {
        val adjuster = DifficultyAdjusterAI()
        feed(adjuster, rounds = 5, success = true, responseTime = 500L)

        // performanceScore = 1.0 * 0.75 + 1.0 * 0.25 = 1.0 -> >= 0.82 -> increase
        assertTrue(
            "expected difficulty > 1 after good performance, got ${adjuster.difficulty}",
            adjuster.difficulty > 1
        )
    }

    @Test
    fun `repeated good performance keeps increasing until cap`() {
        val adjuster = DifficultyAdjusterAI()
        feed(adjuster, rounds = 50, success = true, responseTime = 400L)

        assertEquals(
            "difficulty must be capped at 10",
            10, adjuster.difficulty
        )
    }

    @Test
    fun `low success rate eventually decreases difficulty`() {
        val adjuster = DifficultyAdjusterAI()

        // First climb up
        feed(adjuster, rounds = 20, success = true, responseTime = 500L)
        val peak = adjuster.difficulty
        assertTrue("expected to climb above 1", peak > 1)

        // Then fail enough rounds to flip the success rate below 0.35
        // History size is 8, so 8 failures fully overwrite.
        feed(adjuster, rounds = 8, success = false, responseTime = 4000L)

        assertTrue(
            "expected difficulty to drop after sustained failure (peak=$peak, now=${adjuster.difficulty})",
            adjuster.difficulty < peak
        )
    }

    @Test
    fun `decrease never goes below 1`() {
        val adjuster = DifficultyAdjusterAI()
        feed(adjuster, rounds = 50, success = false, responseTime = 5000L)

        assertEquals(
            "difficulty must be clamped at minimum 1",
            1, adjuster.difficulty
        )
    }

    // -------- Stability / guards --------

    @Test
    fun `does not adjust before having at least 3 samples`() {
        val adjuster = DifficultyAdjusterAI()

        // 2 perfect rounds: not enough history yet
        feed(adjuster, rounds = 2, success = true, responseTime = 400L)

        assertEquals(
            "must remain at 1 with fewer than 3 samples",
            1, adjuster.difficulty
        )
    }

    @Test
    fun `mid-zone performance keeps difficulty stable`() {
        val adjuster = DifficultyAdjusterAI()

        // Alternate success/fail with mid response time
        repeat(8) { idx ->
            adjuster.update(idx % 2 == 0, 1500L)
        }

        // successRate = 0.5, timeScore ~ 0.7 -> performance ~ 0.55 -> stable zone
        assertEquals(
            "mid-zone performance should not change difficulty",
            1, adjuster.difficulty
        )
    }

    // -------- Regression: BUG-002 (NaN with empty history) --------

    @Test
    fun `bug002 regression - no crash when no metrics have been fed`() {
        val adjuster = DifficultyAdjusterAI()

        // Reading difficulty before any update must be safe
        val d = adjuster.difficulty

        assertEquals(1, d)
    }
}
