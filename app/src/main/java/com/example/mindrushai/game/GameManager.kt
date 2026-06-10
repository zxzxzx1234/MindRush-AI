package com.example.mindrushai.game

import com.example.mindrushai.ai.AILogger
import com.example.mindrushai.ai.AIManager
import com.example.mindrushai.ai.DifficultyAdjusterAI
import com.example.mindrushai.ai.llm.LMStudioClient

/**
 * GameManager
 *
 * Coordinates game state, round lifecycle, and AI interactions.
 *
 * Memory game loop:
 *   1. AI generates N words  (N = roundsCompleted + 2, grows every round)
 *   2. Words shown one at a time (speed scales with difficulty)
 *   3. Words hidden — player types them all from memory, in order
 *   4. One wrong word → game over + AI hint
 *   5. Non-English word → soft warning, round continues
 *
 * Progression axes:
 *   • Sequence length = roundsCompleted + 2  (guaranteed +1 per success)
 *   • Word difficulty = DifficultyAdjusterAI  (word profile + display speed)
 *
 * Statistics tracked this session:
 *   score, roundsCompleted, bestScore, currentStreak, bestStreak,
 *   wordsCorrect, wordsAttempted, avgResponseTimeMs
 */
class GameManager {

    private val llmClient    = LMStudioClient()
    private val aiManager    = AIManager(llmClient)
    private val difficultyAI = DifficultyAdjusterAI()

    // ── Game state ────────────────────────────────────────────────────────────

    enum class GameState {
        START, PREPARING_ROUND, SHOWING_SEQUENCE, WAITING_INPUT, ROUND_COMPLETE, GAME_OVER
    }

    var gameState: GameState = GameState.START
        private set

    private val _currentSequence = mutableListOf<String>()
    val currentSequence: List<String> get() = _currentSequence.toList()

    // ── Session statistics ────────────────────────────────────────────────────

    var score: Int = 0
        private set

    var bestScore: Int = 0
        private set

    var roundsCompleted: Int = 0
        private set

    /** Consecutive rounds completed without a mistake. */
    var currentStreak: Int = 0
        private set

    /** Highest streak achieved this session. */
    var bestStreak: Int = 0
        private set

    /** Total words correctly typed this session. */
    var wordsCorrect: Int = 0
        private set

    /** Total word submissions this session (correct + wrong, excludes invalid). */
    var wordsAttempted: Int = 0
        private set

    /** Session accuracy as a float 0..1 */
    val accuracy: Float
        get() = if (wordsAttempted == 0) 0f else wordsCorrect.toFloat() / wordsAttempted

    /** Average per-word response time across the session in ms. */
    val avgResponseTimeMs: Long
        get() = if (allResponseTimes.isEmpty()) 0L else allResponseTimes.average().toLong()

    val difficulty: Int get() = difficultyAI.difficulty
    val currentSequenceLength: Int get() = _currentSequence.size

    var lastHint: String = ""
        private set

    var lastValidationReason: String = ""
        private set

    // Internal state
    private var inputIndex: Int    = 0
    private var wrongAttempts: Int = 0

    private val currentRoundTimes  = mutableListOf<Long>()
    private val allResponseTimes   = mutableListOf<Long>()  // all-session times for avg
    private val successHistory     = ArrayDeque<Boolean>(HISTORY_SIZE)

    // ── Public API ────────────────────────────────────────────────────────────

    suspend fun startGame() {
        reset()
        beginNextRound()
    }

    fun startInputPhase() {
        if (gameState != GameState.SHOWING_SEQUENCE) return
        inputIndex    = 0
        wrongAttempts = 0
        lastHint      = ""
        lastValidationReason = ""
        currentRoundTimes.clear()
        gameState = GameState.WAITING_INPUT
    }

    suspend fun addPlayerInput(inputWord: String, responseMs: Long): InputResult {
        if (gameState != GameState.WAITING_INPUT) return InputResult.ERROR
        if (_currentSequence.isEmpty() || inputIndex >= _currentSequence.size) {
            gameState = GameState.GAME_OVER
            return InputResult.ERROR
        }

        val word = inputWord.trim().lowercase()

        // Word existence check (WordValidatorAI)
        if (word.length > 1) {
            val v = aiManager.validateWord(word)
            if (!v.isValid) {
                lastValidationReason = v.reason
                AILogger.log("WORD_REJECTED", word, v.reason)
                return InputResult.INVALID_WORD
            }
        }

        val expected = _currentSequence[inputIndex]
        wordsAttempted++

        // Sequence match
        if (word != expected) {
            wrongAttempts++
            currentRoundTimes.add(responseMs)
            allResponseTimes.add(responseMs)
            recordResult(false)
            currentStreak = 0
            updateDifficulty()

            lastHint = aiManager.generateHint(
                word          = expected,
                sequence      = _currentSequence.toList(),
                wordIndex     = inputIndex,
                attemptNumber = wrongAttempts
            )

            gameState = GameState.GAME_OVER
            return InputResult.WRONG_WORD
        }

        // Correct word
        wordsCorrect++
        currentRoundTimes.add(responseMs)
        allResponseTimes.add(responseMs)
        inputIndex++
        wrongAttempts        = 0
        lastHint             = ""
        lastValidationReason = ""

        return if (inputIndex >= _currentSequence.size) {
            onRoundSuccess()
            InputResult.ROUND_COMPLETE
        } else {
            InputResult.CORRECT
        }
    }

    fun resetGame() = reset()

    // ── Round lifecycle ───────────────────────────────────────────────────────

    private suspend fun beginNextRound() {
        gameState     = GameState.PREPARING_ROUND
        inputIndex    = 0
        wrongAttempts = 0
        lastHint      = ""
        lastValidationReason = ""
        currentRoundTimes.clear()

        val length = roundsCompleted + 2

        AILogger.log(
            "ROUND_START",
            "round=${roundsCompleted + 1} len=$length d=$difficulty streak=$currentStreak",
            ""
        )

        val words = try {
            aiManager.generateSequence(length, difficulty)
        } catch (e: Exception) {
            AILogger.log("SEQUENCE_EMERGENCY", e.message ?: "?", "")
            emergencySequence(length)
        }

        _currentSequence.clear()
        _currentSequence.addAll(words)
        gameState = GameState.SHOWING_SEQUENCE
    }

    private suspend fun onRoundSuccess() {
        recordResult(true)
        score++
        roundsCompleted++
        currentStreak++
        if (currentStreak > bestStreak) bestStreak = currentStreak
        if (score > bestScore) bestScore = score
        gameState = GameState.ROUND_COMPLETE
        updateDifficulty()
        beginNextRound()
    }

    // ── Difficulty ────────────────────────────────────────────────────────────

    private fun updateDifficulty() {
        val avg = if (currentRoundTimes.isEmpty()) 3000L
        else currentRoundTimes.average().toLong()
        difficultyAI.update(successHistory.lastOrNull() ?: false, avg)
    }

    private fun recordResult(success: Boolean) {
        if (successHistory.size >= HISTORY_SIZE) successHistory.removeFirst()
        successHistory.addLast(success)
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    private fun reset() {
        difficultyAI.reset()
        aiManager.clearValidatorCache()
        score           = 0
        roundsCompleted = 0
        currentStreak   = 0
        bestStreak      = 0
        wordsCorrect    = 0
        wordsAttempted  = 0
        inputIndex      = 0
        wrongAttempts   = 0
        lastHint        = ""
        lastValidationReason = ""
        successHistory.clear()
        currentRoundTimes.clear()
        allResponseTimes.clear()
        _currentSequence.clear()
        gameState = GameState.START
    }

    private fun emergencySequence(length: Int) =
        listOf("cat","dog","sun","map","key","box","hat","cup",
            "ant","jar","log","web","gem","ice","owl","pod")
            .shuffled().take(length)

    enum class InputResult {
        CORRECT,        // correct word, more remain
        ROUND_COMPLETE, // correct word, sequence done
        WRONG_WORD,     // real English but wrong — game over
        INVALID_WORD,   // not real English — soft warning
        ERROR
    }

    companion object {
        private const val HISTORY_SIZE = 5
    }
}