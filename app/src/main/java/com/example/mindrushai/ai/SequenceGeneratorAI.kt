package com.example.mindrushai.ai

class SequenceGeneratorAI {

    fun generateNext(sequence: MutableList<Int>, difficulty: Int): Int {
        return if (difficulty < 5) {
            (0..3).random()
        } else {
            val last = sequence.lastOrNull() ?: 0
            (last + (1..3).random()) % 4
        }
    }
}