package com.example.mindrushai.ai

import android.util.Log

/**
 * In-memory log store for all AI agent decisions.
 *
 * Thread-safety : every public method is @Synchronized — safe for concurrent
 *                 coroutine access from multiple agents.
 * Bounded        : capped at [MAX_ENTRIES] to prevent memory growth during
 *                 long sessions. Oldest entries are dropped when full.
 */
object AILogger {

    private const val TAG        = "MindRushAI"
    private const val MAX_ENTRIES = 500

    private val logs = ArrayDeque<AIDecision>(MAX_ENTRIES)

    @Synchronized
    fun log(decision: AIDecision) {
        if (logs.size >= MAX_ENTRIES) logs.removeFirst()
        logs.addLast(decision)
        Log.d(TAG, format(decision))
    }

    /** Convenience overload — avoids verbose AIDecision(...) construction at call sites. */
    @Synchronized
    fun log(type: String, input: String, output: String) =
        log(AIDecision.of(type, input, output))

    @Synchronized
    fun getLogs(): List<AIDecision> = logs.toList()

    @Synchronized
    fun clear() = logs.clear()

    private fun format(d: AIDecision): String =
        "[${d.type}] in=${d.input.take(80)} → out=${d.output.take(120)}"
}