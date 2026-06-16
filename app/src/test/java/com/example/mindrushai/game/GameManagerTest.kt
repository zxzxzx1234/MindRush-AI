package com.example.mindrushai.game

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [GameManager].
 *
 * GameManager hardcodes its LMStudioClient internally. Since the emulator
 * is not running during unit tests, LLM calls time out and the fallback
 * pools in SequenceGeneratorAI are used — which is exactly the behaviour
 * we want to test (the game must work without a live LLM server).
 *
 * Tests focus on observable state machine behaviour:
 *   - Initial state
 *   - State transitions
 *   - Score and round counting
 *   - Input processing results
 *   - Reset correctness
 */
class GameManagerTest {

    private lateinit var manager: GameManager

    @Before
    fun setUp() {
        manager = GameManager()
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial game state is START`() {
        assertEquals(GameManager.GameState.START, manager.gameState)
    }

    @Test
    fun `initial score is zero`() {
        assertEquals(0, manager.score)
    }

    @Test
    fun `initial rounds completed is zero`() {
        assertEquals(0, manager.roundsCompleted)
    }

    @Test
    fun `initial sequence is empty`() {
        assertTrue(manager.currentSequence.isEmpty())
    }

    @Test
    fun `initial difficulty is one`() {
        assertEquals(1, manager.difficulty)
    }

    // ── startGame ─────────────────────────────────────────────────────────────

    @Test
    fun `startGame transitions to SHOWING_SEQUENCE`() = runBlocking {
        manager.startGame()
        assertEquals(GameManager.GameState.SHOWING_SEQUENCE, manager.gameState)
    }

    @Test
    fun `startGame generates non-empty sequence`() = runBlocking {
        manager.startGame()
        assertTrue(manager.currentSequence.isNotEmpty())
    }

    @Test
    fun `first round sequence has exactly 2 words`() = runBlocking {
        manager.startGame()
        assertEquals(2, manager.currentSequence.size)
    }

    @Test
    fun `startGame resets score to zero`() = runBlocking {
        manager.startGame()
        manager.startGame()
        assertEquals(0, manager.score)
    }

    // ── startInputPhase ───────────────────────────────────────────────────────

    @Test
    fun `startInputPhase transitions SHOWING_SEQUENCE to WAITING_INPUT`() = runBlocking {
        manager.startGame()
        manager.startInputPhase()
        assertEquals(GameManager.GameState.WAITING_INPUT, manager.gameState)
    }

    @Test
    fun `startInputPhase called before startGame is ignored`() {
        manager.startInputPhase()
        assertEquals(GameManager.GameState.START, manager.gameState)
    }

    // ── addPlayerInput — state guard ──────────────────────────────────────────

    @Test
    fun `addPlayerInput before startInputPhase returns ERROR`() = runBlocking {
        manager.startGame()
        val result = manager.addPlayerInput("cat", 500L)
        assertEquals(GameManager.InputResult.ERROR, result)
    }

    // ── addPlayerInput — correct word ─────────────────────────────────────────

    @Test
    fun `correct first word returns CORRECT when sequence has more words`() = runBlocking {
        manager.startGame()
        manager.startInputPhase()
        val first = manager.currentSequence[0]
        val result = manager.addPlayerInput(first, 500L)
        assertEquals(GameManager.InputResult.CORRECT, result)
    }

    @Test
    fun `completing full 2-word sequence returns ROUND_COMPLETE`() = runBlocking {
        manager.startGame()
        manager.startInputPhase()
        val seq = manager.currentSequence
        manager.addPlayerInput(seq[0], 500L)
        val result = manager.addPlayerInput(seq[1], 500L)
        assertEquals(GameManager.InputResult.ROUND_COMPLETE, result)
    }

    @Test
    fun `score increments to 1 after completing first round`() = runBlocking {
        completeRound()
        assertEquals(1, manager.score)
    }

    @Test
    fun `roundsCompleted increments to 1 after first round`() = runBlocking {
        completeRound()
        assertEquals(1, manager.roundsCompleted)
    }

    @Test
    fun `second round sequence has 3 words`() = runBlocking {
        completeRound()
        assertEquals(3, manager.currentSequence.size)
    }

    @Test
    fun `input is trimmed and lowercased`() = runBlocking {
        manager.startGame()
        manager.startInputPhase()
        val first = manager.currentSequence[0]
        val result = manager.addPlayerInput("  ${first.uppercase()}  ", 500L)
        assertNotEquals(GameManager.InputResult.WRONG_WORD, result)
    }

    // ── addPlayerInput — wrong word ───────────────────────────────────────────

    @Test
    fun `wrong real English word returns WRONG_WORD`() = runBlocking {
        manager.startGame()
        manager.startInputPhase()
        val seq = manager.currentSequence
        val wrongWord = if (seq[0] != "apple") "apple" else "orange"
        val result = manager.addPlayerInput(wrongWord, 500L)
        assertEquals(GameManager.InputResult.WRONG_WORD, result)
    }

    @Test
    fun `wrong word sets game state to GAME_OVER`() = runBlocking {
        manager.startGame()
        manager.startInputPhase()
        val seq = manager.currentSequence
        val wrongWord = if (seq[0] != "apple") "apple" else "orange"
        manager.addPlayerInput(wrongWord, 500L)
        assertEquals(GameManager.GameState.GAME_OVER, manager.gameState)
    }

    @Test
    fun `wrong word does not increment score`() = runBlocking {
        manager.startGame()
        manager.startInputPhase()
        val seq = manager.currentSequence
        val wrongWord = if (seq[0] != "apple") "apple" else "orange"
        manager.addPlayerInput(wrongWord, 500L)
        assertEquals(0, manager.score)
    }

    // ── addPlayerInput — invalid word ─────────────────────────────────────────

    @Test
    fun `gibberish word returns INVALID_WORD`() = runBlocking {
        manager.startGame()
        manager.startInputPhase()
        val result = manager.addPlayerInput("xzqwjk", 500L)
        assertEquals(GameManager.InputResult.INVALID_WORD, result)
    }

    @Test
    fun `invalid word does not change state from WAITING_INPUT`() = runBlocking {
        manager.startGame()
        manager.startInputPhase()
        manager.addPlayerInput("xzqwjk", 500L)
        assertEquals(GameManager.GameState.WAITING_INPUT, manager.gameState)
    }

    @Test
    fun `invalid word does not increment score`() = runBlocking {
        manager.startGame()
        manager.startInputPhase()
        manager.addPlayerInput("xzqwjk", 500L)
        assertEquals(0, manager.score)
    }

    // ── resetGame ─────────────────────────────────────────────────────────────

    @Test
    fun `resetGame sets state to START`() = runBlocking {
        manager.startGame()
        manager.resetGame()
        assertEquals(GameManager.GameState.START, manager.gameState)
    }

    @Test
    fun `resetGame clears score`() = runBlocking {
        completeRound()
        manager.resetGame()
        assertEquals(0, manager.score)
    }

    @Test
    fun `resetGame clears roundsCompleted`() = runBlocking {
        completeRound()
        manager.resetGame()
        assertEquals(0, manager.roundsCompleted)
    }

    @Test
    fun `resetGame clears sequence`() = runBlocking {
        manager.startGame()
        manager.resetGame()
        assertTrue(manager.currentSequence.isEmpty())
    }

    @Test
    fun `resetGame clears lastHint`() = runBlocking {
        manager.startGame()
        manager.startInputPhase()
        val seq = manager.currentSequence
        val wrong = if (seq[0] != "apple") "apple" else "orange"
        manager.addPlayerInput(wrong, 500L)
        manager.resetGame()
        assertEquals("", manager.lastHint)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private suspend fun completeRound() {
        manager.startGame()
        manager.startInputPhase()
        val seq = manager.currentSequence.toList()
        for (word in seq) {
            manager.addPlayerInput(word, 500L)
        }
    }
}