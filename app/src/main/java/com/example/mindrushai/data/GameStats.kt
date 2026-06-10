package com.example.mindrushai.data

/**
 * Snapshot of combined session + lifetime statistics.
 * Pure data class — no computed properties (not allowed in Kotlin data class constructors).
 * Accuracy values are pre-computed before construction.
 */
data class GameStats(
    // ── Session (current game) ─────────────────────────────────────────────
    val sessionScore: Int           = 0,
    val sessionRounds: Int          = 0,
    val sessionWordsCorrect: Int    = 0,
    val sessionWordsAttempted: Int  = 0,
    val sessionAccuracy: Float      = 0f,   // pre-computed: correct / attempted
    val sessionBestStreak: Int      = 0,
    val sessionAvgResponseMs: Long  = 0L,

    // ── Lifetime (all sessions) ────────────────────────────────────────────
    val lifetimeBestScore: Int      = 0,
    val lifetimeTotalRounds: Int    = 0,
    val lifetimeTotalGames: Int     = 0,
    val lifetimeWordsCorrect: Int   = 0,
    val lifetimeWordsAttempted: Int = 0,
    val lifetimeAccuracy: Float     = 0f    // pre-computed by ScoreRepository
)