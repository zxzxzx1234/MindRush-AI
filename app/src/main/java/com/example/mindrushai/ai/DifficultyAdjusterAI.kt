package com.example.mindrushai.ai

class DifficultyAdjusterAI {

    var difficulty = 1
        private set

    private val maxDifficulty = 10
    private val minDifficulty = 1

    private val historySize = 6
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
        if (successHistory.isEmpty()) return

        val successRate = successHistory.count { it }.toFloat() / successHistory.size
        val avgTime = responseTimes.average()

        val performanceScore = calculatePerformanceScore(successRate, avgTime)

        when {
            performanceScore > 0.75 -> increaseDifficulty(1)
            performanceScore < 0.35 -> decreaseDifficulty(1)
        }
    }

    private fun calculatePerformanceScore(successRate: Float, avgTime: Double): Double {
        val normalizedTime = when {
            avgTime < 800 -> 1.0
            avgTime < 1500 -> 0.8
            avgTime < 2500 -> 0.6
            avgTime < 3500 -> 0.4
            else -> 0.2
        }

        return (successRate * 0.7) + (normalizedTime * 0.3)
    }

    private fun increaseDifficulty(amount: Int) {
        difficulty = (difficulty + amount).coerceAtMost(maxDifficulty)
    }

    private fun decreaseDifficulty(amount: Int) {
        difficulty = (difficulty - amount).coerceAtLeast(minDifficulty)
    }

    fun reset() {
        difficulty = 1
        successHistory.clear()
        responseTimes.clear()
    }
}