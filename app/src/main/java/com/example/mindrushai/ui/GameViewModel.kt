package com.example.mindrushai.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindrushai.audio.SoundManager
import com.example.mindrushai.data.GameStats
import com.example.mindrushai.data.ScoreRepository
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
 * Translates [GameManager] events into [GameUiState] updates.
 * Also coordinates [ScoreRepository] (persistence) and [SoundManager] (audio).
 */
class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val gameManager  = GameManager()
    private val repository   = ScoreRepository(app)
    private val soundManager = SoundManager()

    private val _uiState = MutableStateFlow(
        GameUiState(bestScore = repository.bestScore)
    )
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var wordInputStartMs: Long = 0L

    // ── User actions ──────────────────────────────────────────────────────────

    fun onStartGame() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    phase           = GamePhase.LOADING,
                    statusText      = "AI is generating words…",
                    inputText       = "",
                    hintText        = "",
                    feedback        = null,
                    wordsTyped      = 0,
                    sequenceSize    = 0,
                    currentStreak   = 0,
                    isNewBestScore  = false,
                    streakMilestone = 0
                )
            }
            gameManager.startGame()
            runSequenceAnimation()
        }
    }

    fun onInputChanged(text: String) {
        _uiState.update {
            it.copy(inputText = text.lowercase().filter { c -> c.isLetter() })
        }
    }

    fun onSubmit() {
        val word = _uiState.value.inputText.trim()
        if (word.isBlank() || !_uiState.value.inputEnabled) return

        viewModelScope.launch {
            _uiState.update { it.copy(inputText = "", isValidating = true) }

            val responseMs = System.currentTimeMillis() - wordInputStartMs
            wordInputStartMs = System.currentTimeMillis()

            val result = gameManager.addPlayerInput(word, responseMs)
            val prevBest = _uiState.value.bestScore

            _uiState.update {
                it.copy(
                    isValidating  = false,
                    score         = gameManager.score,
                    bestScore     = maxOf(it.bestScore, gameManager.bestScore),
                    difficulty    = gameManager.difficulty,
                    currentStreak = gameManager.currentStreak
                )
            }

            when (result) {
                GameManager.InputResult.INVALID_WORD -> {
                    soundManager.playInvalid()
                    showFeedback(FeedbackType.WARNING, "\"$word\" is not a valid English word.")
                }

                GameManager.InputResult.WRONG_WORD -> {
                    soundManager.playWrong()
                    showFeedback(FeedbackType.ERROR, "Wrong word — game over.")

                    repository.saveSessionResult(
                        sessionScore    = gameManager.score,
                        roundsCompleted = gameManager.roundsCompleted,
                        wordsCorrect    = gameManager.wordsCorrect,
                        wordsAttempted  = gameManager.wordsAttempted
                    )

                    val isNewBest = gameManager.score > prevBest && gameManager.score > 0

                    _uiState.update {
                        it.copy(
                            phase           = GamePhase.GAME_OVER,
                            inputEnabled    = false,
                            combo           = 0,
                            currentStreak   = 0,
                            hintText        = gameManager.lastHint,
                            bestScore       = repository.bestScore,
                            isNewBestScore  = isNewBest,
                            streakMilestone = 0
                        )
                    }
                }

                GameManager.InputResult.CORRECT -> {
                    soundManager.playCorrect()
                    val typed     = _uiState.value.wordsTyped + 1
                    val streak    = gameManager.currentStreak
                    val milestone = if (streak > 0 && streak % 5 == 0) streak else 0

                    _uiState.update {
                        it.copy(
                            combo           = it.combo + 1,
                            wordsTyped      = typed,
                            streakMilestone = milestone
                        )
                    }

                    if (milestone > 0) {
                        showFeedback(FeedbackType.SUCCESS, "🔥 $milestone round streak!")
                    } else {
                        showFeedback(
                            FeedbackType.SUCCESS,
                            "✓  Word $typed of ${_uiState.value.sequenceSize}"
                        )
                    }
                }

                GameManager.InputResult.ROUND_COMPLETE -> {
                    soundManager.playRoundComplete()
                    val streak    = gameManager.currentStreak
                    val milestone = if (streak > 0 && streak % 5 == 0) streak else 0

                    _uiState.update { it.copy(combo = it.combo + 1, inputEnabled = false) }

                    if (milestone > 0) {
                        showFeedback(FeedbackType.SUCCESS, "🔥 $milestone round streak!")
                    } else {
                        showFeedback(FeedbackType.SUCCESS, "Round complete! 🎉")
                    }

                    delay(1600)
                    runSequenceAnimation()
                }

                GameManager.InputResult.ERROR -> {
                    _uiState.update {
                        it.copy(phase = GamePhase.GAME_OVER, inputEnabled = false)
                    }
                }
            }
        }
    }

    fun onRestart() {
        gameManager.resetGame()
        _uiState.update { GameUiState(bestScore = repository.bestScore) }
    }

    fun onShowStats() {
        _uiState.update { it.copy(showStatsScreen = true) }
    }

    fun onDismissStats() {
        _uiState.update { it.copy(showStatsScreen = false) }
    }

    /**
     * Builds a [GameStats] snapshot for the Stats screen.
     * Accuracy values are computed here (not in the data class).
     */
    fun buildStats(): GameStats {
        val sessionAttempted = gameManager.wordsAttempted
        val sessionCorrect   = gameManager.wordsCorrect
        val sessionAccuracy  = if (sessionAttempted == 0) 0f
        else sessionCorrect.toFloat() / sessionAttempted

        return GameStats(
            sessionScore           = gameManager.score,
            sessionRounds          = gameManager.roundsCompleted,
            sessionWordsCorrect    = sessionCorrect,
            sessionWordsAttempted  = sessionAttempted,
            sessionAccuracy        = sessionAccuracy,
            sessionBestStreak      = gameManager.bestStreak,
            sessionAvgResponseMs   = gameManager.avgResponseTimeMs,
            lifetimeBestScore      = repository.bestScore,
            lifetimeTotalRounds    = repository.totalRoundsCompleted,
            lifetimeTotalGames     = repository.totalGamesPlayed,
            lifetimeWordsCorrect   = repository.totalWordsCorrect,
            lifetimeWordsAttempted = repository.totalWordsAttempted,
            lifetimeAccuracy       = repository.lifetimeAccuracy
        )
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }

    // ── Sequence animation ────────────────────────────────────────────────────

    private suspend fun runSequenceAnimation() {
        if (gameManager.gameState != GameManager.GameState.SHOWING_SEQUENCE) return

        val sequence  = gameManager.currentSequence
        val seqSize   = sequence.size
        val displayMs = displayTimeMs(gameManager.difficulty)
        val pauseMs   = (displayMs * 0.32).toLong()

        _uiState.update {
            it.copy(
                phase           = GamePhase.MEMORISE,
                inputEnabled    = false,
                displayedWord   = "",
                wordVisible     = false,
                hintText        = "",
                feedback        = null,
                sequenceSize    = seqSize,
                wordsTyped      = 0,
                streakMilestone = 0,
                statusText      = "Round ${gameManager.roundsCompleted + 1}  ·  Memorise $seqSize word${if (seqSize != 1) "s" else ""}:"
            )
        }

        delay(500)

        for ((index, word) in sequence.withIndex()) {
            soundManager.playWordAppear()
            _uiState.update {
                it.copy(
                    displayedWord = word,
                    wordVisible   = true,
                    statusText    = "Word ${index + 1} of $seqSize"
                )
            }
            delay(displayMs)
            _uiState.update { it.copy(wordVisible = false) }
            delay(pauseMs)
        }

        _uiState.update { it.copy(displayedWord = "") }
        delay(350)

        gameManager.startInputPhase()
        wordInputStartMs = System.currentTimeMillis()

        _uiState.update {
            it.copy(
                phase        = GamePhase.RECALL,
                statusText   = "Now type all $seqSize word${if (seqSize != 1) "s" else ""} in order:",
                inputEnabled = true,
                difficulty   = gameManager.difficulty,
                wordsTyped   = 0
            )
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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