package com.example.mindrushai.game
import com.example.mindrushai.ai.SequenceGeneratorAI
import com.example.mindrushai.ai.DifficultyAdjusterAI

class GameManager {

    private val generator = SequenceGeneratorAI()
    private val difficultyAI = DifficultyAdjusterAI()
    enum class GameState {
        START, PLAYING, GAME_OVER
    }

    var gameState = GameState.START
    var currentSequence = mutableListOf<Int>()
    var playerInput = mutableListOf<Int>()
    var score = 0

    fun startGame() {
        gameState = GameState.PLAYING
        score = 0
        currentSequence.clear()
        nextRound()
    }

    fun nextRound() {
        playerInput.clear()
        val next = generator.generateNext(currentSequence, difficultyAI.difficulty)
        currentSequence.add(next)
    }

    fun addPlayerInput(value: Int, responseTime: Long): Boolean {
        playerInput.add(value)

        val index = playerInput.lastIndex
        if (playerInput[index] != currentSequence[index]) {
            difficultyAI.update(false, responseTime)
            gameState = GameState.GAME_OVER
            return false
        }

        if (playerInput.size == currentSequence.size) {
            score++
            difficultyAI.update(true, responseTime)
            nextRound()
        }

        return true
    }

    fun resetGame() {
        gameState = GameState.START
        currentSequence.clear()
        playerInput.clear()
        score = 0
    }
}