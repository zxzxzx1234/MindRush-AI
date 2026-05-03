package com.example.mindrushai.ai

class DifficultyAdjusterAI {

    var difficulty = 1
        private set

    private val maxDifficulty = 10
    private val minDifficulty = 1

    private val historySize = 8
    private val successHistory = ArrayDeque<Boolean>()
    private val responseTimes = ArrayDeque<Long>()

    fun update(success: Boolean, responseTime: Long) {
        updateHistory(success, responseTime)
        adjustDifficulty()
    }

    private fun updateHistory(success: Boolean, responseTime: Long) {
        if (successHistory.size >= historySize) {
            successHistory.removeFirst()
            responseTimes.removeFirst()
        }

        successHistory.addLast(success)
        responseTimes.addLast(responseTime)
    }

    private fun adjustDifficulty() {
        if (successHistory.size < 3) return

        val successRate =
            successHistory.count { it }.toFloat() / successHistory.size

        val avgTime = responseTimes.average()

        val performanceScore =
            calculatePerformanceScore(successRate, avgTime)

        when {
            performanceScore >= 0.82 -> increaseDifficulty(1)
            performanceScore <= 0.35 -> decreaseDifficulty(1)
        }
    }

    private fun calculatePerformanceScore(
        successRate: Float,
        avgTime: Double
    ): Double {

        val timeScore = when {
            avgTime < 700 -> 1.0
            avgTime < 1200 -> 0.85
            avgTime < 1800 -> 0.7
            avgTime < 2500 -> 0.5
            avgTime < 3500 -> 0.3
            else -> 0.1
        }

        return (successRate * 0.75) + (timeScore * 0.25)
    }

    private fun increaseDifficulty(amount: Int) {
        difficulty =
            (difficulty + amount).coerceAtMost(maxDifficulty)
    }

    private fun decreaseDifficulty(amount: Int) {
        difficulty =
            (difficulty - amount).coerceAtLeast(minDifficulty)
    }

    fun reset() {
        difficulty = 1
        successHistory.clear()
        responseTimes.clear()
    }
}