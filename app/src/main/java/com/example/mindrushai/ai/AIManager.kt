package com.example.mindrushai.ai

import com.example.mindrushai.ai.llm.LLMClient

/**
 * AIManager
 *
 * Single entry point for all AI functionality consumed by GameManager.
 * Orchestrates the three non-deterministic AI agents, all sharing one
 * [LLMClient] instance to avoid redundant HTTP connections.
 *
 * Agents:
 *   1. [SequenceGeneratorAI] — generates phonetically distinct words per round
 *   2. [WordValidatorAI]     — checks whether player input is a real English word
 *   3. [HintGeneratorAI]     — produces contextual memory hints after failure
 *
 * [DifficultyAdjusterAI] is deterministic and lives directly in GameManager.
 */
class AIManager(llmClient: LLMClient? = null) {

    private val sequenceGenerator = SequenceGeneratorAI(llmClient)
    private val wordValidator     = WordValidatorAI(llmClient)
    private val hintGenerator     = HintGeneratorAI(llmClient)

    // ── Agent delegation ──────────────────────────────────────────────────────

    /**
     * Generates [length] words optimised for memorability at [difficulty].
     * Called once per round — always a fresh LLM call, no caching.
     */
    suspend fun generateSequence(length: Int, difficulty: Int): List<String> =
        sequenceGenerator.generateSequence(length, difficulty)

    /**
     * Returns whether [word] is a valid English word.
     * Uses session cache + offline dictionary before calling the LLM,
     * so frequently-typed correct words cost zero latency after the first check.
     */
    suspend fun validateWord(word: String): WordValidatorAI.ValidationResult =
        wordValidator.validate(word)

    /**
     * Generates a contextual hint for [word] after the player failed to recall it.
     *
     * @param word          The word that should have been typed.
     * @param sequence      The full round sequence (for positional context).
     * @param wordIndex     Index of [word] within [sequence].
     * @param attemptNumber Consecutive failures on this word — controls explicitness.
     */
    suspend fun generateHint(
        word: String,
        sequence: List<String> = emptyList(),
        wordIndex: Int = 0,
        attemptNumber: Int = 1
    ): String = hintGenerator.generateHint(word, sequence, wordIndex, attemptNumber)

    /**
     * Clears the word validator's session cache.
     * Must be called at the start of each new game.
     */
    fun clearValidatorCache() = wordValidator.clearCache()
}