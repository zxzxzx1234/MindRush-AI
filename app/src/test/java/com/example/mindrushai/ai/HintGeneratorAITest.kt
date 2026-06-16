package com.example.mindrushai.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [HintGeneratorAI].
 *
 * Tests local fallback behaviour (no LLM) and LLM integration
 * via FakeLLMClient.
 */
class HintGeneratorAITest {

    // ── Local fallback (no LLM) ───────────────────────────────────────────────

    @Test
    fun `hint is non-empty without LLM`() = runBlocking {
        val gen = HintGeneratorAI(llmClient = null)
        val hint = gen.generateHint("cat")
        assertTrue(hint.isNotBlank())
    }

    @Test
    fun `attempt 1 returns generic hint for short word`() = runBlocking {
        val gen = HintGeneratorAI(llmClient = null)
        val hint = gen.generateHint("cat", attemptNumber = 1)
        assertTrue(hint.isNotBlank())
        // Short word (<=4 letters) gets a "short word" hint
        assertTrue(
            "Expected hint about short word, got: $hint",
            hint.contains("short", ignoreCase = true) ||
                    hint.contains("common", ignoreCase = true) ||
                    hint.contains("everyday", ignoreCase = true)
        )
    }

    @Test
    fun `attempt 2 reveals first letter and length`() = runBlocking {
        val gen = HintGeneratorAI(llmClient = null)
        val hint = gen.generateHint("blanket", attemptNumber = 2)
        assertTrue(
            "Expected hint to mention first letter 'b', got: $hint",
            hint.contains("b", ignoreCase = true)
        )
        assertTrue(
            "Expected hint to mention length 7, got: $hint",
            hint.contains("7")
        )
    }

    @Test
    fun `attempt 3 or more reveals first letter explicitly`() = runBlocking {
        val gen = HintGeneratorAI(llmClient = null)
        val hint = gen.generateHint("ephemeral", attemptNumber = 3)
        assertTrue(
            "Expected hint to mention first letter 'e', got: $hint",
            hint.contains("'e'", ignoreCase = true) ||
                    hint.contains("e", ignoreCase = true)
        )
        assertTrue(
            "Expected hint to mention length 9, got: $hint",
            hint.contains("9")
        )
    }

    @Test
    fun `medium length word gets medium hint at attempt 1`() = runBlocking {
        val gen = HintGeneratorAI(llmClient = null)
        val hint = gen.generateHint("blanket", attemptNumber = 1)
        assertTrue(hint.isNotBlank())
    }

    @Test
    fun `long word gets long word hint at attempt 1`() = runBlocking {
        val gen = HintGeneratorAI(llmClient = null)
        val hint = gen.generateHint("labyrinth", attemptNumber = 1)
        assertTrue(
            "Expected hint about long word, got: $hint",
            hint.contains("long", ignoreCase = true) ||
                    hint.contains("uncommon", ignoreCase = true) ||
                    hint.contains("less common", ignoreCase = true)
        )
    }

    // ── With sequence context ─────────────────────────────────────────────────

    @Test
    fun `hint works with empty sequence`() = runBlocking {
        val gen = HintGeneratorAI(llmClient = null)
        val hint = gen.generateHint("sun", emptyList(), 0, 1)
        assertTrue(hint.isNotBlank())
    }

    @Test
    fun `hint works with single word sequence`() = runBlocking {
        val gen = HintGeneratorAI(llmClient = null)
        val hint = gen.generateHint("sun", listOf("sun"), 0, 1)
        assertTrue(hint.isNotBlank())
    }

    @Test
    fun `hint works with multi word sequence at various positions`() = runBlocking {
        val gen = HintGeneratorAI(llmClient = null)
        val sequence = listOf("cat", "sun", "map", "key")
        for (i in sequence.indices) {
            val hint = gen.generateHint(sequence[i], sequence, i, 1)
            assertTrue("Expected non-empty hint at index $i", hint.isNotBlank())
        }
    }

    // ── LLM integration ───────────────────────────────────────────────────────

    @Test
    fun `LLM response is used when available`() = runBlocking {
        val expected = "Think of a maze you cannot escape."
        val gen = HintGeneratorAI(llmClient = FakeLLMClient(expected))
        val hint = gen.generateHint("labyrinth", attemptNumber = 1)
        assertEquals(expected, hint)
    }

    @Test
    fun `LLM response has surrounding quotes stripped`() = runBlocking {
        val gen = HintGeneratorAI(llmClient = FakeLLMClient("\"Think of a maze.\""))
        val hint = gen.generateHint("labyrinth", attemptNumber = 1)
        assertFalse("Hint should not start with quote", hint.startsWith("\""))
        assertFalse("Hint should not end with quote", hint.endsWith("\""))
    }

    @Test
    fun `empty LLM response falls back to local hint`() = runBlocking {
        val gen = HintGeneratorAI(llmClient = FakeLLMClient(""))
        val hint = gen.generateHint("cat", attemptNumber = 1)
        assertTrue(hint.isNotBlank())
    }

    @Test
    fun `LLM exception falls back to local hint`() = runBlocking {
        val gen = HintGeneratorAI(llmClient = ThrowingLLMClient())
        val hint = gen.generateHint("cat", attemptNumber = 1)
        assertTrue(hint.isNotBlank())
    }

    @Test
    fun `LLM only called once per generateHint call`() = runBlocking {
        val fake = FakeLLMClient("A furry pet.")
        val gen = HintGeneratorAI(llmClient = fake)
        gen.generateHint("cat", attemptNumber = 1)
        assertEquals(1, fake.callCount)
    }
}