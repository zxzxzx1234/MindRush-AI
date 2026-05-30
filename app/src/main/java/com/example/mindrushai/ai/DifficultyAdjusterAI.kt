package com.example.mindrushai.ai

/**
 * DifficultyAdjusterAI
 *
 * Deterministic, local difficulty engine — no LLM, always instant.
 *
 * What difficulty controls:
 *   - Word profile : short/common (low) → rare/long (high)
 *   - Display speed: words flash faster at higher difficulty
 *
 * What difficulty does NOT control:
 *   - Sequence length — grows by 1 after every successful round,
 *     independently of difficulty (handled in GameManager)
 *
 * Reacts after EVERY round with no minimum sample requirement,
 * so the player feels progression from the very first round.
 *
 * Algorithm:
 *   score = (successRate × 0.75) + (timeScore × 0.25)
 *   score ≥ [THRESHOLD_UP]   → difficulty + 1
 *   score ≤ [THRESHOLD_DOWN] → difficulty − 1
 *   otherwise                → hold
 */
class DifficultyAdjusterAI {

    companion object {
        const val MIN_DIFFICULTY = 1
        const val MAX_DIFFICULTY = 10
        private const val HISTORY_SIZE   = 5     // smaller = faster reaction
        private const val THRESHOLD_UP   = 0.80
        private const val THRESHOLD_DOWN = 0.40
    }

    var difficulty: Int = MIN_DIFFICULTY
        private set

    private val successHistory = ArrayDeque<Boolean>(HISTORY_SIZE)
    private val responseTimes  = ArrayDeque<Long>(HISTORY_SIZE)

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Records a round result and immediately recomputes difficulty.
     *
     * @param success        Whether the round was completed correctly.
     * @param responseTimeMs Average per-word response time in ms.
     */
    fun update(success: Boolean, responseTimeMs: Long) {
        append(success, responseTimeMs)
        recalculate()
    }

    fun reset() {
        difficulty = MIN_DIFFICULTY
        successHistory.clear()
        responseTimes.clear()
    }

    fun snapshot(): PerformanceSnapshot {
        val sr = if (successHistory.isEmpty()) 0f
        else successHistory.count { it }.toFloat() / successHistory.size
        val at = if (responseTimes.isEmpty()) 0.0 else responseTimes.average()
        return PerformanceSnapshot(
            difficulty        = difficulty,
            successRate       = sr,
            avgResponseTimeMs = at,
            sampleCount       = successHistory.size,
            performanceScore  = performanceScore(sr, at)
        )
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun append(success: Boolean, responseTimeMs: Long) {
        if (successHistory.size >= HISTORY_SIZE) {
            successHistory.removeFirst()
            responseTimes.removeFirst()
        }
        successHistory.addLast(success)
        responseTimes.addLast(responseTimeMs)
    }

    private fun recalculate() {
        val sr    = successHistory.count { it }.toFloat() / successHistory.size
        val at    = responseTimes.average()
        val score = performanceScore(sr, at)
        val prev  = difficulty

        when {
            score >= THRESHOLD_UP   -> difficulty = (difficulty + 1).coerceAtMost(MAX_DIFFICULTY)
            score <= THRESHOLD_DOWN -> difficulty = (difficulty - 1).coerceAtLeast(MIN_DIFFICULTY)
        }

        val tag = if (difficulty != prev) "DIFFICULTY_CHANGED" else "DIFFICULTY_HELD"
        AILogger.log(tag,
            "sr=${"%.2f".format(sr)} t=${at.toLong()}ms score=${"%.3f".format(score)}",
            if (difficulty != prev) "$prev → $difficulty" else difficulty.toString()
        )
    }

    private fun performanceScore(successRate: Float, avgTimeMs: Double): Double {
        val timeScore = when {
            avgTimeMs < 800  -> 1.00
            avgTimeMs < 1500 -> 0.85
            avgTimeMs < 2500 -> 0.65
            avgTimeMs < 4000 -> 0.40
            avgTimeMs < 6000 -> 0.20
            else             -> 0.05
        }
        return (successRate * 0.75) + (timeScore * 0.25)
    }

    data class PerformanceSnapshot(
        val difficulty: Int,
        val successRate: Float,
        val avgResponseTimeMs: Double,
        val sampleCount: Int,
        val performanceScore: Double
    )
}