package com.example.mindrushai.game

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [GameManager].
 *
 * GameManager wires up the real AIManager with an LMStudio client. In a unit-test
 * environment LM Studio is unreachable, so the manager falls back to its local
 * heuristic generator — exactly the path documented in BUGS.md (BUG-001).
 *
 * Coverage:
 *  - Initial state and reset
 *  - startGame populates a non-empty sequence and ends in WAITING_INPUT after startInputPhase()
 *  - Correct input increments index and eventually advances the round
 *  - Wrong input ends the game (GAME_OVER)
 *  - addPlayerInput in the wrong state is rejected
 *  - resetGame brings everything back to defaults
 */
class GameManagerTest {

    @Test
    fun `initial state is START with score 0 and difficulty 1`() {
        val gm = GameManager()

        assertEquals(GameManager.GameState.START, gm.gameState)
        assertEquals(0, gm.score)
        assertEquals(0, gm.roundsCompleted)
        assertEquals(1, gm.difficulty)
        assertTrue(gm.currentSequence.isEmpty())
    }

    @Test
    fun `startGame produces a non-empty sequence`() = runTest {
        val gm = GameManager()
        gm.startGame()

        assertTrue(
            "sequence must be populated after startGame, got ${gm.currentSequence}",
            gm.currentSequence.isNotEmpty()
        )
        gm.currentSequence.forEach {
            assertTrue("invalid token $it", it in 0..3)
        }
    }

    @Test
    fun `startInputPhase moves SHOWING_SEQUENCE to WAITING_INPUT`() = runTest {
        val gm = GameManager()
        gm.startGame()

        // After startGame, state is SHOWING_SEQUENCE
        assertEquals(GameManager.GameState.SHOWING_SEQUENCE, gm.gameState)

        gm.startInputPhase()

        assertEquals(GameManager.GameState.WAITING_INPUT, gm.gameState)
    }

    @Test
    fun `startInputPhase is a no-op when not in SHOWING_SEQUENCE`() {
        val gm = GameManager()

        // Currently in START
        gm.startInputPhase()

        assertEquals(GameManager.GameState.START, gm.gameState)
    }

    @Test
    fun `addPlayerInput is rejected outside WAITING_INPUT`() = runTest {
        val gm = GameManager()
        gm.startGame() // SHOWING_SEQUENCE

        val accepted = gm.addPlayerInput(value = 0, responseTime = 500L)

        assertFalse(
            "input should be rejected when state is ${gm.gameState}",
            accepted
        )
    }

    @Test
    fun `wrong first input ends the game`() = runTest {
        val gm = GameManager()
        gm.startGame()
        gm.startInputPhase()

        val expected = gm.currentSequence.first()
        val wrong = (expected + 1) % 4

        val accepted = gm.addPlayerInput(value = wrong, responseTime = 600L)

        assertFalse(accepted)
        assertEquals(GameManager.GameState.GAME_OVER, gm.gameState)
        assertEquals("score must stay 0 on first-input failure", 0, gm.score)
    }

    @Test
    fun `correct full sequence increments score and starts a new round`() = runTest {
        val gm = GameManager()
        gm.startGame()
        gm.startInputPhase()

        // Difficulty 1 → length 1, so a single correct tap completes the round.
        val expected = gm.currentSequence.toList()
        val initialScore = gm.score

        for (v in expected) {
            val ok = gm.addPlayerInput(value = v, responseTime = 700L)
            assertTrue("expected accept on value=$v", ok)
        }

        assertEquals(initialScore + 1, gm.score)
        assertEquals(1, gm.roundsCompleted)

        // After onRoundSuccess, the manager kicks off a new round.
        // It should NOT be in GAME_OVER.
        assertNotEquals(GameManager.GameState.GAME_OVER, gm.gameState)
    }

    @Test
    fun `resetGame brings everything back to defaults`() = runTest {
        val gm = GameManager()
        gm.startGame()
        gm.startInputPhase()

        // Force GAME_OVER via wrong input
        val wrong = (gm.currentSequence.first() + 1) % 4
        gm.addPlayerInput(wrong, 600L)
        assertEquals(GameManager.GameState.GAME_OVER, gm.gameState)

        gm.resetGame()

        assertEquals(GameManager.GameState.START, gm.gameState)
        assertEquals(0, gm.score)
        assertEquals(0, gm.roundsCompleted)
        assertEquals(1, gm.difficulty)
        assertTrue(gm.currentSequence.isEmpty())
    }

    @Test
    fun `bug001 regression - game proceeds even when LLM is unreachable`() = runTest {
        // GameManager constructs LMStudioClient pointing to localhost.
        // In a unit-test JVM there is no LM Studio running, so every LLM call must fail
        // and the manager must fall back to the local generator transparently.
        val gm = GameManager()
        gm.startGame()

        assertTrue(
            "fallback path must produce a sequence, got ${gm.currentSequence}",
            gm.currentSequence.isNotEmpty()
        )
        assertEquals(GameManager.GameState.SHOWING_SEQUENCE, gm.gameState)
    }
}
