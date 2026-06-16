package com.example.mindrushai.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [SequenceGeneratorAI].
 *
 * LLM-dependent paths are tested with a [FakeLLMClient] that returns
 * controlled responses, allowing deterministic verification of:
 *   - correct word count returned
 *   - fallback activation on timeout/empty response
 *   - parser robustness against messy LLM output
 *   - difficulty-appropriate pool selection
 */
class SequenceGeneratorAITest {

    // ── No-client (fallback only) ─────────────────────────────────────────────

    @Test
    fun `generateSequence with no client returns correct length`() = runBlocking {
        val gen = SequenceGeneratorAI(llmClient = null)
        val result = gen.generateSequence(3, 1)
        assertEquals(3, result.size)
    }

    @Test
    fun `generateSequence with no client returns non-empty strings`() = runBlocking {
        val gen = SequenceGeneratorAI(llmClient = null)
        val result = gen.generateSequence(4, 1)
        assertTrue(result.all { it.isNotBlank() })
    }

    @Test
    fun `generateSequence length zero returns empty list`() = runBlocking {
        val gen = SequenceGeneratorAI(llmClient = null)
        val result = gen.generateSequence(0, 1)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `generateSequence returns no duplicates`() = runBlocking {
        val gen = SequenceGeneratorAI(llmClient = null)
        val result = gen.generateSequence(5, 1)
        assertEquals(result.size, result.distinct().size)
    }

    // ── Difficulty pool selection ─────────────────────────────────────────────

    @Test
    fun `difficulty 1-3 returns short words from easy pool`() = runBlocking {
        val gen = SequenceGeneratorAI(llmClient = null)
        for (d in 1..3) {
            val result = gen.generateSequence(3, d)
            assertTrue(
                "Expected short words at difficulty $d, got: $result",
                result.all { it.length in 2..4 }
            )
        }
    }

    @Test
    fun `difficulty 4-6 returns medium words`() = runBlocking {
        val gen = SequenceGeneratorAI(llmClient = null)
        for (d in 4..6) {
            val result = gen.generateSequence(3, d)
            assertTrue(
                "Expected medium words at difficulty $d, got: $result",
                result.all { it.length >= 5 }
            )
        }
    }

    @Test
    fun `difficulty 7-10 returns long words`() = runBlocking {
        val gen = SequenceGeneratorAI(llmClient = null)
        for (d in 7..10) {
            val result = gen.generateSequence(3, d)
            assertTrue(
                "Expected long words at difficulty $d, got: $result",
                result.all { it.length >= 7 }
            )
        }
    }

    // ── LLM client — clean response ───────────────────────────────────────────

    @Test
    fun `LLM clean response is parsed correctly`() = runBlocking {
        val fake = FakeLLMClient("cat,dog,sun")
        val gen = SequenceGeneratorAI(llmClient = fake)
        val result = gen.generateSequence(3, 1)
        assertEquals(listOf("cat", "dog", "sun"), result)
    }

    @Test
    fun `LLM response with spaces is trimmed`() = runBlocking {
        val fake = FakeLLMClient("  cat , dog , sun  ")
        val gen = SequenceGeneratorAI(llmClient = fake)
        val result = gen.generateSequence(3, 1)
        assertEquals(listOf("cat", "dog", "sun"), result)
    }

    @Test
    fun `LLM response is lowercased`() = runBlocking {
        val fake = FakeLLMClient("Cat,DOG,Sun")
        val gen = SequenceGeneratorAI(llmClient = fake)
        val result = gen.generateSequence(3, 1)
        assertEquals(listOf("cat", "dog", "sun"), result)
    }

    // ── LLM client — messy response ───────────────────────────────────────────

    @Test
    fun `LLM response with numbering is parsed`() = runBlocking {
        val fake = FakeLLMClient("1. cat\n2. dog\n3. sun")
        val gen = SequenceGeneratorAI(llmClient = fake)
        val result = gen.generateSequence(3, 1)
        assertEquals(3, result.size)
        assertTrue(result.containsAll(listOf("cat", "dog", "sun")))
    }

    @Test
    fun `LLM response with brackets is parsed`() = runBlocking {
        val fake = FakeLLMClient("[cat, dog, sun]")
        val gen = SequenceGeneratorAI(llmClient = fake)
        val result = gen.generateSequence(3, 1)
        assertTrue(result.containsAll(listOf("cat", "dog", "sun")))
    }

    // ── LLM client — fallback activation ─────────────────────────────────────

    @Test
    fun `empty LLM response activates fallback`() = runBlocking {
        val fake = FakeLLMClient("")
        val gen = SequenceGeneratorAI(llmClient = fake)
        val result = gen.generateSequence(3, 1)
        assertEquals(3, result.size)
        assertTrue(result.all { it.isNotBlank() })
    }

    @Test
    fun `LLM response shorter than expected is supplemented by fallback`() = runBlocking {
        val fake = FakeLLMClient("cat,dog")   // only 2 words for request of 4
        val gen = SequenceGeneratorAI(llmClient = fake)
        val result = gen.generateSequence(4, 1)
        assertEquals(4, result.size)
    }

    @Test
    fun `exception from LLM client activates fallback`() = runBlocking {
        val fake = ThrowingLLMClient()
        val gen = SequenceGeneratorAI(llmClient = fake)
        val result = gen.generateSequence(3, 1)
        assertEquals(3, result.size)
    }
}