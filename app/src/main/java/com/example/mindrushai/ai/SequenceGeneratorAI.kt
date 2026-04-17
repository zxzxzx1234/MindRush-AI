package com.example.mindrushai.ai

import kotlin.random.Random

class SequenceGeneratorAI {

    private val random = Random(System.currentTimeMillis())

    fun generateNext(sequence: List<Int>, difficulty: Int): Int {
        return when (difficulty) {
            in 1..2 -> randomStrategy()
            in 3..4 -> patternStrategy(sequence)
            in 5..6 -> variationStrategy(sequence)
            else -> adaptiveStrategy(sequence)
        }
    }

    fun generateSequence(length: Int, difficulty: Int): List<Int> {
        val sequence = mutableListOf<Int>()

        repeat(length) {
            val next = generateSmartValue(sequence, difficulty)
            sequence.add(next)
        }

        return sequence
    }

    fun refineSequence(sequence: List<Int>): List<Int> {

        if (sequence.isEmpty()) return sequence

        val refined = mutableListOf<Int>()

        for (i in sequence.indices) {

            val value = sequence[i]

            if (i > 0 && value == sequence[i - 1]) {
                val alternatives = (0..3).filter { it != value }
                refined.add(alternatives.random())
            } else {
                refined.add(value)
            }
        }

        return refined
    }

    private fun generateSmartValue(sequence: List<Int>, difficulty: Int): Int {

        val allValues = (0..3).toList()

        if (sequence.isEmpty()) {
            return allValues.random()
        }

        val last = sequence.last()

        val candidates = mutableListOf<Int>()

        for (value in allValues) {

            var score = 0

            if (value == last) score -= 3

            if (sequence.size >= 2 && value == sequence[sequence.size - 2]) score -= 2

            val diversityBonus = sequence.toSet().size
            score += diversityBonus

            if (difficulty >= 5) {
                if ((value - last + 4) % 4 == 1) score += 2
            }

            repeat(score.coerceAtLeast(1)) {
                candidates.add(value)
            }
        }

        return if (candidates.isNotEmpty()) {
            candidates.random()
        } else {
            allValues.random()
        }
    }

    private fun randomStrategy(): Int {
        return random.nextInt(0, 4)
    }

    private fun patternStrategy(sequence: List<Int>): Int {
        if (sequence.isEmpty()) return randomStrategy()

        val last = sequence.last()
        val direction = if (random.nextBoolean()) 1 else -1
        return (last + direction + 4) % 4
    }

    private fun variationStrategy(sequence: List<Int>): Int {
        if (sequence.size < 2) return patternStrategy(sequence)

        val last = sequence.last()
        val secondLast = sequence[sequence.size - 2]

        val diff = (last - secondLast + 4) % 4
        return (last + diff + 4) % 4
    }

    private fun adaptiveStrategy(sequence: List<Int>): Int {
        if (sequence.size < 3) return variationStrategy(sequence)

        return when (detectPattern(sequence)) {
            Pattern.REPEAT -> (sequence.last() + 1) % 4
            Pattern.ALTERNATE -> sequence[sequence.size - 2]
            Pattern.INCREMENT -> (sequence.last() + 1) % 4
            Pattern.RANDOM -> randomStrategy()
        }
    }

    private fun detectPattern(sequence: List<Int>): Pattern {
        val last = sequence[sequence.size - 1]
        val second = sequence[sequence.size - 2]
        val third = sequence[sequence.size - 3]

        return when {
            last == second -> Pattern.REPEAT
            last == third -> Pattern.ALTERNATE
            (last - second + 4) % 4 == (second - third + 4) % 4 -> Pattern.INCREMENT
            else -> Pattern.RANDOM
        }
    }

    private enum class Pattern {
        REPEAT,
        ALTERNATE,
        INCREMENT,
        RANDOM
    }
}