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
 * ── Memory game loop ─────────────────────────────────────────────────────────
 *
 *   Each round:
 *     1. AI generates N words  (N = roundsCompleted + 2, grows every round)
 *     2. Words shown one at a time for a timed display (speed scales with difficulty)
 *     3. Words hidden — player types them all from memory, in order
 *     4. One wrong word → game over + AI-generated hint
 *     5. Non-English word → soft warning, round continues (no penalty)
 *
 *   Two independent progression axes:
 *     • Sequence length  = roundsCompleted + 2  (guaranteed +1 every success)
 *     • Word difficulty  = DifficultyAdjusterAI  (controls word profile + speed)
 *
 * ── State machine ────────────────────────────────────────────────────────────
 *
 *   START → PREPARING_ROUND → SHOWING_SEQUENCE → WAITING_INPUT
 *                                                  ├─ ROUND_COMPLETE → PREPARING_ROUND
 *                                                  └─ GAME_OVER
 */
class GameManager {

    private val llmClient    = LMStudioClient()
    private val aiManager    = AIManager(llmClient)
    private val difficultyAI = DifficultyAdjusterAI()

    // ── State ─────────────────────────────────────────────────────────────────

    enum class GameState {
        START,
        PREPARING_ROUND,
        SHOWING_SEQUENCE,
        WAITING_INPUT,
        ROUND_COMPLETE,
        GAME_OVER
    }

    var gameState: GameState = GameState.START
        private set

    private val _currentSequence = mutableListOf<String>()

    /** Immutable snapshot of the current round's word sequence. */
    val currentSequence: List<String> get() = _currentSequence.toList()

    var score: Int = 0
        private set

    var roundsCompleted: Int = 0
        private set

    /** Current word-profile difficulty, driven by [DifficultyAdjusterAI]. */
    val difficulty: Int get() = difficultyAI.difficulty

    /** Total words in the current sequence (= roundsCompleted + 2). */
    val currentSequenceLength: Int get() = _currentSequence.size

    private var inputIndex: Int    = 0
    private var wrongAttempts: Int = 0

    /** Last hint from [HintGeneratorAI]. Shown on the Game Over screen. */
    var lastHint: String = ""
        private set

    /** Reason a word was rejected by [WordValidatorAI]. Shown as inline feedback. */
    var lastValidationReason: String = ""
        private set

    // Per-word response times for the current round (for difficulty calculation)
    private val currentRoundTimes = mutableListOf<Long>()

    // Bounded performance history
    private val successHistory = ArrayDeque<Boolean>(HISTORY_SIZE)

    // ── Public API ────────────────────────────────────────────────────────────

    /** Resets everything and begins the first round. */
    suspend fun startGame() {
        reset()
        beginNextRound()
    }

    /**
     * Called by the UI once the word animation finishes.
     * Transitions SHOWING_SEQUENCE → WAITING_INPUT.
     */
    fun startInputPhase() {
        if (gameState != GameState.SHOWING_SEQUENCE) return
        inputIndex    = 0
        wrongAttempts = 0
        lastHint      = ""
        lastValidationReason = ""
        currentRoundTimes.clear()
        gameState = GameState.WAITING_INPUT
    }

    /**
     * Processes a single word typed by the player.
     *
     * Steps:
     *   1. Guard: reject if state ≠ WAITING_INPUT.
     *   2. WordValidatorAI: confirm the word exists in English.
     *      Non-English → INVALID_WORD (round continues, no penalty).
     *   3. Sequence match: compare against expected word.
     *      Wrong → HintGeneratorAI → GAME_OVER.
     *   4. Advance. Sequence complete → ROUND_COMPLETE → next round.
     *
     * @param inputWord  Player's submission (trimmed + lowercased internally).
     * @param responseMs Ms since the player started typing this word.
     */
    suspend fun addPlayerInput(inputWord: String, responseMs: Long): InputResult {
        if (gameState != GameState.WAITING_INPUT) return InputResult.ERROR

        if (_currentSequence.isEmpty() || inputIndex >= _currentSequence.size) {
            gameState = GameState.GAME_OVER
            return InputResult.ERROR
        }

        val word = inputWord.trim().lowercase()

        // ── Step 2: word existence check ──────────────────────────────────────
        if (word.length > 1) {
            val validation = aiManager.validateWord(word)
            if (!validation.isValid) {
                lastValidationReason = validation.reason
                AILogger.log("WORD_REJECTED", word, validation.reason)
                return InputResult.INVALID_WORD
            }
        }

        val expected = _currentSequence[inputIndex]

        // ── Step 3: sequence match ────────────────────────────────────────────
        if (word != expected) {
            wrongAttempts++
            currentRoundTimes.add(responseMs)
            recordResult(success = false)
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

        // ── Step 4: correct word ──────────────────────────────────────────────
        currentRoundTimes.add(responseMs)
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

    /** Resets all session data without starting a new round. */
    fun resetGame() = reset()

    // ── Round lifecycle ───────────────────────────────────────────────────────

    private suspend fun beginNextRound() {
        gameState     = GameState.PREPARING_ROUND
        inputIndex    = 0
        wrongAttempts = 0
        lastHint      = ""
        lastValidationReason = ""
        currentRoundTimes.clear()

        // Sequence grows by 1 every completed round — always starts at 2
        val length = roundsCompleted + 2

        AILogger.log(
            "ROUND_START",
            "round=${roundsCompleted + 1} seqLen=$length difficulty=$difficulty",
            ""
        )

        val words = try {
            aiManager.generateSequence(length, difficulty)
        } catch (e: Exception) {
            AILogger.log("SEQUENCE_EMERGENCY_FALLBACK", e.message ?: "?", "")
            emergencySequence(length)
        }

        _currentSequence.clear()
        _currentSequence.addAll(words)
        gameState = GameState.SHOWING_SEQUENCE
    }

    private suspend fun onRoundSuccess() {
        recordResult(success = true)
        score++
        roundsCompleted++
        gameState = GameState.ROUND_COMPLETE
        updateDifficulty()
        beginNextRound()
    }

    // ── Difficulty ────────────────────────────────────────────────────────────

    private fun updateDifficulty() {
        val avgTime = if (currentRoundTimes.isEmpty()) 3000L
        else currentRoundTimes.average().toLong()
        difficultyAI.update(
            success        = successHistory.lastOrNull() ?: false,
            responseTimeMs = avgTime
        )
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
        inputIndex      = 0
        wrongAttempts   = 0
        lastHint        = ""
        lastValidationReason = ""
        successHistory.clear()
        currentRoundTimes.clear()
        _currentSequence.clear()
        gameState = GameState.START
    }

    // ── Emergency fallback ────────────────────────────────────────────────────

    private fun emergencySequence(length: Int): List<String> =
        listOf("cat","dog","sun","map","key","box","hat","cup",
            "ant","jar","log","web","gem","ice","owl","pod")
            .shuffled().take(length)

    // ── Result enum ───────────────────────────────────────────────────────────

    enum class InputResult {
        /** Correct word; more words remain. */
        CORRECT,
        /** Correct word; sequence complete — round over. */
        ROUND_COMPLETE,
        /** Real English word but wrong — game over. */
        WRONG_WORD,
        /** Not a real English word — soft warning, round continues. */
        INVALID_WORD,
        /** Called from an invalid game state. */
        ERROR
    }

    companion object {
        private const val HISTORY_SIZE = 5
    }
}