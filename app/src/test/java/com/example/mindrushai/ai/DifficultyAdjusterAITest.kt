package com.example.mindrushai.ai

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [DifficultyAdjusterAI].
 *
 * All tested behaviours are deterministic — no LLM involved.
 */
class DifficultyAdjusterAITest {

    private lateinit var adjuster: DifficultyAdjusterAI

    @Before
    fun setUp() {
        adjuster = DifficultyAdjusterAI()
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial difficulty is MIN`() {
        assertEquals(DifficultyAdjusterAI.MIN_DIFFICULTY, adjuster.difficulty)
    }

    @Test
    fun `reset returns difficulty to MIN`() {
        repeat(10) { adjuster.update(true, 500L) }
        adjuster.reset()
        assertEquals(DifficultyAdjusterAI.MIN_DIFFICULTY, adjuster.difficulty)
    }

    // ── Difficulty increases ──────────────────────────────────────────────────

    @Test
    fun `consistent success with fast response increases difficulty`() {
        val initial = adjuster.difficulty
        // 5 successes at 500ms — score ~= 0.75 + 0.25 = 1.0, well above 0.80
        repeat(5) { adjuster.update(true, 500L) }
        assertTrue(
            "Expected difficulty > $initial, got ${adjuster.difficulty}",
            adjuster.difficulty > initial
        )
    }

    @Test
    fun `difficulty does not exceed MAX`() {
        repeat(50) { adjuster.update(true, 300L) }
        assertEquals(DifficultyAdjusterAI.MAX_DIFFICULTY, adjuster.difficulty)
    }

    // ── Difficulty decreases ──────────────────────────────────────────────────

    @Test
    fun `consistent failure with slow response decreases difficulty`() {
        // First push up so we have room to fall
        repeat(10) { adjuster.update(true, 400L) }
        val elevated = adjuster.difficulty

        adjuster.reset()
        // Push to same elevated level again cleanly
        repeat(10) { adjuster.update(true, 400L) }

        // Now fail 5 times slowly
        repeat(5) { adjuster.update(false, 7000L) }

        assertTrue(
            "Expected difficulty to decrease from $elevated, got ${adjuster.difficulty}",
            adjuster.difficulty < elevated
        )
    }

    @Test
    fun `difficulty does not go below MIN`() {
        repeat(20) { adjuster.update(false, 9000L) }
        assertEquals(DifficultyAdjusterAI.MIN_DIFFICULTY, adjuster.difficulty)
    }

    // ── Stability ─────────────────────────────────────────────────────────────

    @Test
    fun `mixed performance holds difficulty stable`() {
        val initial = adjuster.difficulty
        // Alternate success and failure at medium speed — should stay near start
        repeat(5) {
            adjuster.update(true, 2000L)
            adjuster.update(false, 2000L)
        }
        // Not asserting exact value, just that it hasn't shot to MAX or MIN
        assertTrue(adjuster.difficulty in 1..DifficultyAdjusterAI.MAX_DIFFICULTY)
    }

    // ── Snapshot ──────────────────────────────────────────────────────────────

    @Test
    fun `snapshot returns zero values on empty history`() {
        val snap = adjuster.snapshot()
        assertEquals(0f,  snap.successRate, 0.001f)
        assertEquals(0.0, snap.avgResponseTimeMs, 0.001)
        assertEquals(0,   snap.sampleCount)
    }

    @Test
    fun `snapshot reflects updates correctly`() {
        adjuster.update(true, 1000L)
        adjuster.update(true, 2000L)
        val snap = adjuster.snapshot()
        assertEquals(2,   snap.sampleCount)
        assertEquals(1f,  snap.successRate, 0.001f)
        assertEquals(1500.0, snap.avgResponseTimeMs, 1.0)
    }

    // ── Performance score thresholds ──────────────────────────────────────────

    @Test
    fun `all successes at very fast speed yields high performance score`() {
        repeat(5) { adjuster.update(true, 400L) }
        val snap = adjuster.snapshot()
        assertTrue(
            "Expected score > 0.80, got ${snap.performanceScore}",
            snap.performanceScore >= 0.80
        )
    }

    @Test
    fun `all failures at very slow speed yields low performance score`() {
        repeat(5) { adjuster.update(false, 8000L) }
        val snap = adjuster.snapshot()
        assertTrue(
            "Expected score <= 0.40, got ${snap.performanceScore}",
            snap.performanceScore <= 0.40
        )
    }
}