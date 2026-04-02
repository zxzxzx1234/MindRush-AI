package com.example.mindrushai.ai

class DifficultyAdjusterAI {

    var difficulty = 1

    fun update(success: Boolean, responseTime: Long) {
        if (success && responseTime < 2000) {
            difficulty++
        } else if (!success) {
            difficulty = maxOf(1, difficulty - 1)
        }
    }
}