package com.example.mindrushai.ui

/**
 * Complete UI state for the game screen.
 * Immutable — every update goes through ViewModel.copy().
 */
data class GameUiState(
    // ── Stats ──────────────────────────────────────────────────────────────
    val score: Int          = 0,
    val bestScore: Int      = 0,
    val difficulty: Int     = 1,
    val combo: Int          = 0,
    val currentStreak: Int  = 0,

    // ── Round progress ─────────────────────────────────────────────────────
    val sequenceSize: Int = 0,
    val wordsTyped: Int   = 0,

    // ── Phase ──────────────────────────────────────────────────────────────
    val phase: GamePhase = GamePhase.IDLE,

    // ── Word display ───────────────────────────────────────────────────────
    val displayedWord: String = "",
    val wordVisible: Boolean  = false,

    // ── Input ──────────────────────────────────────────────────────────────
    val inputText: String     = "",
    val inputEnabled: Boolean = false,
    val isValidating: Boolean = false,

    // ── Feedback & hint ────────────────────────────────────────────────────
    val feedback: Feedback? = null,
    val hintText: String    = "",

    // ── Status ─────────────────────────────────────────────────────────────
    val statusText: String = "Press Start to play",

    // ── Navigation ─────────────────────────────────────────────────────────
    val showStatsScreen: Boolean = false,

    // ── Celebrations ───────────────────────────────────────────────────────
    /** True for one frame when the player achieves a new personal best. */
    val isNewBestScore: Boolean = false,

    /** True when streak reaches a milestone (5, 10, 15…). */
    val streakMilestone: Int = 0   // 0 = no milestone, >0 = milestone value
)

enum class GamePhase {
    IDLE,       // before first game
    LOADING,    // AI generating words
    MEMORISE,   // showing words one by one
    RECALL,     // player typing from memory
    GAME_OVER   // round failed
}

data class Feedback(val type: FeedbackType, val message: String)

enum class FeedbackType { SUCCESS, WARNING, ERROR }