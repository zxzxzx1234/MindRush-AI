package com.example.mindrushai.data

import android.content.Context
import android.content.SharedPreferences

/**
 * ScoreRepository
 *
 * Persists gameplay statistics across app sessions using SharedPreferences.
 * All writes are applied immediately via [commit] for synchronous safety.
 *
 * Stored data:
 *   - Best score ever achieved
 *   - Total rounds completed across all sessions
 *   - Total games played
 *   - Total words correctly recalled
 *   - Total words attempted
 */
class ScoreRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Read ──────────────────────────────────────────────────────────────────

    val bestScore: Int
        get() = prefs.getInt(KEY_BEST_SCORE, 0)

    val totalRoundsCompleted: Int
        get() = prefs.getInt(KEY_TOTAL_ROUNDS, 0)

    val totalGamesPlayed: Int
        get() = prefs.getInt(KEY_TOTAL_GAMES, 0)

    val totalWordsCorrect: Int
        get() = prefs.getInt(KEY_WORDS_CORRECT, 0)

    val totalWordsAttempted: Int
        get() = prefs.getInt(KEY_WORDS_ATTEMPTED, 0)

    val lifetimeAccuracy: Float
        get() {
            val attempted = totalWordsAttempted
            return if (attempted == 0) 0f
            else totalWordsCorrect.toFloat() / attempted
        }

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Updates stats after a game session ends.
     * Only updates best score if [sessionScore] is a new high.
     */
    fun saveSessionResult(
        sessionScore: Int,
        roundsCompleted: Int,
        wordsCorrect: Int,
        wordsAttempted: Int
    ) {
        prefs.edit().apply {
            if (sessionScore > bestScore) putInt(KEY_BEST_SCORE, sessionScore)
            putInt(KEY_TOTAL_ROUNDS, totalRoundsCompleted + roundsCompleted)
            putInt(KEY_TOTAL_GAMES,  totalGamesPlayed + 1)
            putInt(KEY_WORDS_CORRECT,   totalWordsCorrect + wordsCorrect)
            putInt(KEY_WORDS_ATTEMPTED, totalWordsAttempted + wordsAttempted)
            apply()
        }
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    // ── Companion ─────────────────────────────────────────────────────────────

    companion object {
        private const val PREFS_NAME         = "mindrush_scores"
        private const val KEY_BEST_SCORE      = "best_score"
        private const val KEY_TOTAL_ROUNDS    = "total_rounds"
        private const val KEY_TOTAL_GAMES     = "total_games"
        private const val KEY_WORDS_CORRECT   = "words_correct"
        private const val KEY_WORDS_ATTEMPTED = "words_attempted"
    }
}