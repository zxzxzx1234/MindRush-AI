package com.example.mindrushai.ai

import com.example.mindrushai.ai.llm.LLMClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class AIManager(
    private val llmClient: LLMClient? = null
) {

    private val generator = SequenceGeneratorAI()

    suspend fun generateSequence(length: Int, difficulty: Int): List<Int> {

        if (llmClient == null) {
            return generator.generateSequence(length, difficulty)
        }

        return try {

            val prompt = """
Generate a sequence for a memory game.

Rules:
- Length: $length
- Difficulty: $difficulty
- Values must be integers between 0 and 3
- Avoid repeating the same number too often
- Make it challenging but fair

Return ONLY numbers like: 0,1,2,3
            """.trimIndent()

            val response = withContext(Dispatchers.IO) {
                withTimeoutOrNull(1500) {
                    llmClient.generate(prompt)
                }
            } ?: return generator.generateSequence(length, difficulty)

            val parsed = parseSequence(response, length)

            generator.refineSequence(parsed, difficulty)

        } catch (e: Exception) {

            generator.generateSequence(length, difficulty)
        }
    }

    suspend fun adjustDifficulty(
        currentDifficulty: Int,
        successRate: Float,
        avgTime: Double
    ): Int {

        return if (llmClient != null) {
            tryLLMAdjustment(currentDifficulty, successRate, avgTime)
        } else {
            heuristicAdjustment(currentDifficulty, successRate, avgTime)
        }
    }

    suspend fun explainDecision(
        difficulty: Int,
        successRate: Float
    ): String {

        return if (llmClient != null) {

            val prompt = """
Player success rate: $successRate
New difficulty: $difficulty

Explain briefly in ONE short sentence.
            """.trimIndent()

            withContext(Dispatchers.IO) {
                withTimeoutOrNull(1200) {
                    llmClient.generate(prompt)
                }
            } ?: "Adjusted based on performance."

        } else {
            "Difficulty adjusted based on performance."
        }
    }

    private fun heuristicAdjustment(
        difficulty: Int,
        successRate: Float,
        avgTime: Double
    ): Int {

        return when {
            successRate > 0.85 && avgTime < 1200 -> difficulty + 1
            successRate < 0.4 -> difficulty - 1
            else -> difficulty
        }.coerceIn(1, 10)
    }

    private suspend fun tryLLMAdjustment(
        difficulty: Int,
        successRate: Float,
        avgTime: Double
    ): Int {

        return try {

            val prompt = """
You control game difficulty.

Current difficulty: $difficulty
Success rate: $successRate
Average response time: $avgTime ms

Return ONLY a number between 1 and 10.
            """.trimIndent()

            val response = withContext(Dispatchers.IO) {
                withTimeoutOrNull(1200) {
                    llmClient!!.generate(prompt)
                }
            } ?: return heuristicAdjustment(difficulty, successRate, avgTime)

            response.trim()
                .toIntOrNull()
                ?.coerceIn(1, 10)
                ?: heuristicAdjustment(difficulty, successRate, avgTime)

        } catch (e: Exception) {

            heuristicAdjustment(difficulty, successRate, avgTime)
        }
    }

    private fun parseSequence(
        response: String,
        expectedLength: Int
    ): List<Int> {

        val numbers = response
            .replace("[", "")
            .replace("]", "")
            .split(",", " ")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 0..3 }

        return if (numbers.size >= expectedLength) {
            numbers.take(expectedLength)
        } else {
            generator.generateSequence(expectedLength, 1)
        }
    }
}