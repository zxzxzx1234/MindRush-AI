package com.example.mindrushai.game

import com.example.mindrushai.ai.AIManager
import com.example.mindrushai.ai.DifficultyAdjusterAI
import com.example.mindrushai.ai.llm.LMStudioClient

class GameManager {

    private val aiManager = AIManager(LMStudioClient())
    private val difficultyAI = DifficultyAdjusterAI()

    enum class GameState {
        START,
        SHOWING_SEQUENCE,
        WAITING_INPUT,
        GAME_OVER
    }

    var gameState = GameState.START
        private set

    private val _currentSequence = mutableListOf<Int>()
    val currentSequence: List<Int> get() = _currentSequence

    var score = 0
        private set

    private var currentDifficulty = 1

    val difficulty: Int
        get() = currentDifficulty

    private var currentInputIndex = 0

    private val successHistory = mutableListOf<Boolean>()
    private val responseTimes = mutableListOf<Long>()

    fun startGame() {
        difficultyAI.reset()
        score = 0
        currentDifficulty = 1
        successHistory.clear()
        responseTimes.clear()
        startNewRound()
    }

    private fun startNewRound() {
        generateSequence()
        currentInputIndex = 0
        gameState = GameState.SHOWING_SEQUENCE
    }

    private fun generateSequence() {
        val length = currentDifficulty

        val newSequence = try {
            aiManager.generateSequence(length, currentDifficulty)
        } catch (e: Exception) {
            List(length) { (0..3).random() }
        }

        _currentSequence.clear()
        _currentSequence.addAll(newSequence)
    }

    fun startInputPhase() {
        if (gameState == GameState.SHOWING_SEQUENCE) {
            currentInputIndex = 0
            gameState = GameState.WAITING_INPUT
        }
    }

    fun addPlayerInput(value: Int, responseTime: Long): Boolean {
        if (gameState != GameState.WAITING_INPUT) return false

        if (value != _currentSequence[currentInputIndex]) {
            registerResult(false, responseTime)
            gameState = GameState.GAME_OVER
            return false
        }

        currentInputIndex++

        if (currentInputIndex >= _currentSequence.size) {
            registerResult(true, responseTime)
            onRoundSuccess()
        }

        return true
    }

    private fun onRoundSuccess() {
        score++
        updateDifficulty()
        startNewRound()
    }

    private fun registerResult(success: Boolean, responseTime: Long) {
        if (successHistory.size >= 5) {
            successHistory.removeAt(0)
            responseTimes.removeAt(0)
        }

        successHistory.add(success)
        responseTimes.add(responseTime)
    }

    private fun updateDifficulty() {
        if (successHistory.isEmpty()) return

        val successRate = successHistory.count { it }.toFloat() / successHistory.size
        val avgTime = responseTimes.average()

        currentDifficulty = try {
            aiManager.adjustDifficulty(currentDifficulty, successRate, avgTime)
        } catch (e: Exception) {
            difficultyAI.update(true, avgTime.toLong())
            difficultyAI.difficulty
        }
    }

    fun resetGame() {
        _currentSequence.clear()
        score = 0
        currentInputIndex = 0
        currentDifficulty = 1
        successHistory.clear()
        responseTimes.clear()
        difficultyAI.reset()
        gameState = GameState.START
    }
}