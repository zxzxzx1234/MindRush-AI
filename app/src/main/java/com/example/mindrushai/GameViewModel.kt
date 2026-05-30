package com.example.mindrushai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindrushai.game.GameManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * GameViewModel
 *
 * Translates [GameManager] events into [GameUiState] updates consumed by the UI.
 * Survives screen rotation. All coroutines are tied to [viewModelScope] and are
 * automatically cancelled when the ViewModel is cleared.
 */
class GameViewModel : ViewModel() {

    private val gameManager = GameManager()

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // Timestamp when the player started typing the current word
    private var wordInputStartMs: Long = 0L

    // ── User actions ──────────────────────────────────────────────────────────

    fun onStartGame() {
        viewModelScope.launch {
            _uiState.update { it.copy(
                isLoading    = true,
                statusText   = "AI is generating words…",
                gameOver     = false,
                inputText    = "",
                hintText     = "",
                feedback     = null,
                wordsTyped   = 0,
                sequenceSize = 0
            )}
            gameManager.startGame()
            runSequenceAnimation()
        }
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(
            inputText = text.lowercase().filter { c -> c.isLetter() }
        )}
    }

    fun onSubmit() {
        val word = _uiState.value.inputText.trim()
        if (word.isBlank() || !_uiState.value.inputEnabled) return

        viewModelScope.launch {
            _uiState.update { it.copy(inputText = "", isValidating = true) }

            val responseMs = System.currentTimeMillis() - wordInputStartMs
            wordInputStartMs = System.currentTimeMillis()

            val result = gameManager.addPlayerInput(word, responseMs)

            _uiState.update { it.copy(
                isValidating = false,
                score        = gameManager.score,
                difficulty   = gameManager.difficulty
            )}

            when (result) {
                GameManager.InputResult.INVALID_WORD -> {
                    showFeedback(FeedbackType.WARNING, "\"$word\" is not a valid English word.")
                }

                GameManager.InputResult.WRONG_WORD -> {
                    showFeedback(FeedbackType.ERROR, "Wrong word — game over.")
                    _uiState.update { it.copy(
                        gameOver     = true,
                        inputEnabled = false,
                        combo        = 0,
                        hintText     = gameManager.lastHint
                    )}
                }

                GameManager.InputResult.CORRECT -> {
                    val typed = _uiState.value.wordsTyped + 1
                    _uiState.update { it.copy(
                        combo      = it.combo + 1,
                        wordsTyped = typed
                    )}
                    showFeedback(FeedbackType.SUCCESS, "✓  Word $typed of ${_uiState.value.sequenceSize}")
                }

                GameManager.InputResult.ROUND_COMPLETE -> {
                    _uiState.update { it.copy(
                        combo        = it.combo + 1,
                        inputEnabled = false
                    )}
                    showFeedback(FeedbackType.SUCCESS, "Round complete! 🎉")
                    delay(1600)
                    runSequenceAnimation()
                }

                GameManager.InputResult.ERROR -> {
                    _uiState.update { it.copy(gameOver = true, inputEnabled = false) }
                }
            }
        }
    }

    fun onRestart() {
        gameManager.resetGame()
        _uiState.update { GameUiState() }
    }

    // ── Sequence animation ────────────────────────────────────────────────────

    private suspend fun runSequenceAnimation() {
        if (gameManager.gameState != GameManager.GameState.SHOWING_SEQUENCE) return

        val sequence  = gameManager.currentSequence
        val seqSize   = sequence.size
        val displayMs = displayTimeMs(gameManager.difficulty)
        val pauseMs   = (displayMs * 0.35).toLong()

        _uiState.update { it.copy(
            isLoading     = false,
            inputEnabled  = false,
            displayedWord = "",
            wordVisible   = false,
            hintText      = "",
            feedback      = null,
            sequenceSize  = seqSize,
            wordsTyped    = 0,
            statusText    = "Round ${gameManager.roundsCompleted + 1} — memorise $seqSize word${if (seqSize != 1) "s" else ""}:"
        )}

        delay(500)

        for ((index, word) in sequence.withIndex()) {
            _uiState.update { it.copy(
                displayedWord = word,
                wordVisible   = true,
                statusText    = "Word ${index + 1} of $seqSize"
            )}
            delay(displayMs)
            _uiState.update { it.copy(wordVisible = false) }
            delay(pauseMs)
        }

        _uiState.update { it.copy(displayedWord = "") }
        delay(400)

        gameManager.startInputPhase()

        wordInputStartMs = System.currentTimeMillis()
        _uiState.update { it.copy(
            statusText   = "Now type all $seqSize word${if (seqSize != 1) "s" else ""} in order:",
            inputEnabled = true,
            difficulty   = gameManager.difficulty,
            wordsTyped   = 0
        )}
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Word display duration in ms — decreases with higher difficulty. */
    private fun displayTimeMs(difficulty: Int): Long = when (difficulty) {
        in 1..2 -> 1800L
        in 3..4 -> 1400L
        in 5..6 -> 1000L
        in 7..8 -> 750L
        else    -> 550L
    }

    private fun showFeedback(type: FeedbackType, message: String) {
        _uiState.update { it.copy(feedback = Feedback(type, message)) }
        viewModelScope.launch {
            delay(2200)
            if (_uiState.value.feedback?.message == message) {
                _uiState.update { it.copy(feedback = null) }
            }
        }
    }
}

// ── UI state ──────────────────────────────────────────────────────────────────

data class GameUiState(
    val score: Int         = 0,
    val difficulty: Int    = 1,
    val combo: Int         = 0,

    /** Total words in this round's sequence. */
    val sequenceSize: Int  = 0,

    /** How many the player has correctly typed so far this round. */
    val wordsTyped: Int    = 0,

    val statusText: String = "Press Start to play",

    val displayedWord: String = "",
    val wordVisible: Boolean  = false,

    val inputText: String     = "",
    val inputEnabled: Boolean = false,
    val isLoading: Boolean    = false,
    val isValidating: Boolean = false,

    val feedback: Feedback? = null,
    val hintText: String    = "",
    val gameOver: Boolean   = false
)

data class Feedback(val type: FeedbackType, val message: String)

enum class FeedbackType { SUCCESS, WARNING, ERROR }