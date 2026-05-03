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

        if (length <= 0) return emptyList()

        val sequence = mutableListOf<Int>()

        repeat(length) {
            val next = generateSmartValue(sequence, difficulty)
            sequence.add(next)
        }

        return refineSequence(sequence, difficulty)
    }

    fun refineSequence(
        sequence: List<Int>,
        difficulty: Int
    ): List<Int> {

        if (sequence.isEmpty()) return sequence

        val refined = mutableListOf<Int>()

        for (i in sequence.indices) {

            val value = sequence[i]

            if (i > 0 && value == refined.last()) {

                val alternatives = (0..3)
                    .filter { it != value }

                val best = alternatives.maxByOrNull {
                    scoreCandidate(refined, it, difficulty)
                }

                refined.add(best ?: alternatives.random())

            } else {
                refined.add(value)
            }
        }

        return refined
    }

    private fun generateSmartValue(
        sequence: List<Int>,
        difficulty: Int
    ): Int {

        val allValues = (0..3).toList()

        if (sequence.isEmpty()) {
            return allValues.random(random)
        }

        val candidatePool = mutableListOf<Int>()

        for (value in allValues) {

            val score = scoreCandidate(sequence, value, difficulty)
                .coerceAtLeast(1)

            repeat(score) {
                candidatePool.add(value)
            }
        }

        return if (candidatePool.isNotEmpty()) {
            candidatePool.random(random)
        } else {
            allValues.random(random)
        }
    }

    private fun scoreCandidate(
        sequence: List<Int>,
        value: Int,
        difficulty: Int
    ): Int {

        var score = 5

        val last = sequence.lastOrNull()

        // penalizare repetiții directe
        if (last != null && value == last) {
            score -= 4
        }

        // penalizare pattern repetitiv (ABAB)
        if (sequence.size >= 2 &&
            value == sequence[sequence.size - 2]
        ) {
            score -= 2
        }

        // bonus diversitate
        val diversityBonus =
            4 - sequence.takeLast(4).count { it == value }

        score += diversityBonus

        // dificultate medie → încurajează progresie
        if (difficulty >= 5 && last != null) {
            if ((value - last + 4) % 4 == 1) {
                score += 2
            }
        }

        // dificultate mare → evită repetări recente
        if (difficulty >= 7) {
            val recent = sequence.takeLast(3)
            if (value !in recent) {
                score += 3
            }
        }

        return score.coerceAtLeast(1)
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

        if (sequence.size < 3) {
            return variationStrategy(sequence)
        }

        return when (detectPattern(sequence)) {
            Pattern.REPEAT ->
                (sequence.last() + 1) % 4

            Pattern.ALTERNATE ->
                sequence[sequence.size - 2]

            Pattern.INCREMENT ->
                (sequence.last() + 1) % 4

            Pattern.RANDOM ->
                generateSmartValue(sequence, 8)
        }
    }

    private fun detectPattern(sequence: List<Int>): Pattern {

        val last = sequence[sequence.size - 1]
        val second = sequence[sequence.size - 2]
        val third = sequence[sequence.size - 3]

        return when {
            last == second -> Pattern.REPEAT
            last == third -> Pattern.ALTERNATE
            (last - second + 4) % 4 ==
                    (second - third + 4) % 4 ->
                Pattern.INCREMENT

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