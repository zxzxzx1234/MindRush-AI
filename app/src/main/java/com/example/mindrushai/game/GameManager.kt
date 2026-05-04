package com.example.mindrushai.game

import com.example.mindrushai.ai.AIManager
import com.example.mindrushai.ai.DifficultyAdjusterAI
import com.example.mindrushai.ai.llm.LMStudioClient

class GameManager {

    private val aiManager = AIManager(LMStudioClient())
    private val difficultyAI = DifficultyAdjusterAI()

    enum class GameState {
        START,
        PREPARING_ROUND,
        SHOWING_SEQUENCE,
        WAITING_INPUT,
        ROUND_COMPLETE,
        GAME_OVER
    }

    var gameState = GameState.START
        private set

    private val _currentSequence = mutableListOf<Int>()
    val currentSequence: List<Int>
        get() = _currentSequence

    var score = 0
        private set

    var roundsCompleted = 0
        private set

    private var currentDifficulty = 1

    val difficulty: Int
        get() = currentDifficulty

    private var currentInputIndex = 0

    private val successHistory = mutableListOf<Boolean>()
    private val responseTimes = mutableListOf<Long>()

    private val maxHistorySize = 8

    suspend fun startGame() {
        resetSessionStats()
        startNewRound()
    }

    private fun resetSessionStats() {
        difficultyAI.reset()
        score = 0
        roundsCompleted = 0
        currentDifficulty = 1
        currentInputIndex = 0
        successHistory.clear()
        responseTimes.clear()
        _currentSequence.clear()
        gameState = GameState.START
    }

    private suspend fun startNewRound() {
        gameState = GameState.PREPARING_ROUND
        generateSequence()
        currentInputIndex = 0
        gameState = GameState.SHOWING_SEQUENCE
    }

    private suspend fun generateSequence() {
        val length = currentDifficulty

        val generatedSequence = try {
            aiManager.generateSequence(length, currentDifficulty)
        } catch (e: Exception) {
            fallbackSequence(length)
        }

        _currentSequence.clear()
        _currentSequence.addAll(generatedSequence)
    }

    private fun fallbackSequence(length: Int): List<Int> {
        return List(length) { (0..3).random() }
    }

    fun startInputPhase() {
        if (gameState != GameState.SHOWING_SEQUENCE) return
        currentInputIndex = 0
        gameState = GameState.WAITING_INPUT
    }

    suspend fun addPlayerInput(
        value: Int,
        responseTime: Long
    ): Boolean {

        if (gameState != GameState.WAITING_INPUT) {
            return false
        }

        if (_currentSequence.isEmpty()) {
            gameState = GameState.GAME_OVER
            return false
        }

        if (currentInputIndex >= _currentSequence.size) {
            gameState = GameState.GAME_OVER
            return false
        }

        val expectedValue =
            _currentSequence[currentInputIndex]

        if (value != expectedValue) {
            onRoundFailure(responseTime)
            return false
        }

        currentInputIndex++

        registerInputTime(responseTime)

        if (currentInputIndex >= _currentSequence.size) {
            onRoundSuccess()
        }

        return true
    }

    private suspend fun onRoundSuccess() {
        registerRoundResult(true)
        score++
        roundsCompleted++
        gameState = GameState.ROUND_COMPLETE
        updateDifficulty()
        startNewRound()
    }

    private suspend fun onRoundFailure(
        responseTime: Long
    ) {
        registerInputTime(responseTime)
        registerRoundResult(false)
        updateDifficulty()
        gameState = GameState.GAME_OVER
    }

    private fun registerRoundResult(
        success: Boolean
    ) {
        if (successHistory.size >= maxHistorySize) {
            successHistory.removeAt(0)
        }
        successHistory.add(success)
    }

    private fun registerInputTime(
        responseTime: Long
    ) {
        if (responseTimes.size >= maxHistorySize) {
            responseTimes.removeAt(0)
        }
        responseTimes.add(responseTime)
    }

    private suspend fun updateDifficulty() {

        if (successHistory.isEmpty() ||
            responseTimes.isEmpty()
        ) {
            return
        }

        val successRate =
            successHistory.count { it }.toFloat() /
                    successHistory.size

        val averageTime =
            responseTimes.average()

        currentDifficulty = try {
            aiManager.adjustDifficulty(
                currentDifficulty,
                successRate,
                averageTime
            )
        } catch (e: Exception) {

            difficultyAI.update(
                successHistory.last(),
                averageTime.toLong()
            )

            difficultyAI.difficulty
        }
    }

    fun resetGame() {
        resetSessionStats()
    }
}