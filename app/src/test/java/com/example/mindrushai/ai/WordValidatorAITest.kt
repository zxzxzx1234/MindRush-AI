package com.example.mindrushai.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [WordValidatorAI].
 *
 * Covers all three tiers of validation:
 *   Tier 1 — instant accept (single letter, cache, offline dict)
 *   Tier 2 — instant reject (invalid cache)
 *   Tier 3 — LLM call (via FakeLLMClient)
 */
class WordValidatorAITest {

    private lateinit var validator: WordValidatorAI

    @Before
    fun setUp() {
        validator = WordValidatorAI(llmClient = null)
    }

    // ── Empty / single letter ─────────────────────────────────────────────────

    @Test
    fun `empty string is rejected`() = runBlocking {
        assertFalse(validator.validate("").isValid)
    }

    @Test
    fun `blank string with spaces is rejected`() = runBlocking {
        assertFalse(validator.validate("   ").isValid)
    }

    @Test
    fun `single letter a is accepted`() = runBlocking {
        assertTrue(validator.validate("a").isValid)
    }

    @Test
    fun `single letter i is accepted`() = runBlocking {
        assertTrue(validator.validate("i").isValid)
    }

    @Test
    fun `single letter x is accepted`() = runBlocking {
        assertTrue(validator.validate("x").isValid)
    }

    // ── Offline dictionary ────────────────────────────────────────────────────

    @Test
    fun `common word cat is valid offline`() = runBlocking {
        assertTrue(validator.validate("cat").isValid)
    }

    @Test
    fun `common word blanket is valid offline`() = runBlocking {
        assertTrue(validator.validate("blanket").isValid)
    }

    @Test
    fun `hard word ephemeral is valid offline`() = runBlocking {
        assertTrue(validator.validate("ephemeral").isValid)
    }

    @Test
    fun `input is trimmed before lookup`() = runBlocking {
        assertTrue(validator.validate("  cat  ").isValid)
    }

    @Test
    fun `input is lowercased before lookup`() = runBlocking {
        assertTrue(validator.validate("CAT").isValid)
        assertTrue(validator.validate("Blanket").isValid)
    }

    @Test
    fun `gibberish is rejected offline`() = runBlocking {
        assertFalse(validator.validate("xzqwjk").isValid)
    }

    @Test
    fun `random letter string is rejected offline`() = runBlocking {
        assertFalse(validator.validate("aaabbbccc").isValid)
    }

    // ── Session cache ─────────────────────────────────────────────────────────

    @Test
    fun `valid word is cached after first lookup`() = runBlocking {
        val fake = FakeLLMClient("YES: real word")
        val v = WordValidatorAI(llmClient = fake)

        v.validate("flibbertigibbet")
        v.validate("flibbertigibbet")
        v.validate("flibbertigibbet")

        assertEquals("LLM should be called once then cached", 1, fake.callCount)
    }

    @Test
    fun `invalid word is cached after first rejection`() = runBlocking {
        val fake = FakeLLMClient("NO: not a word")
        val v = WordValidatorAI(llmClient = fake)

        v.validate("xyzqwk999")
        v.validate("xyzqwk999")

        assertEquals("LLM should be called once then cached", 1, fake.callCount)
    }

    @Test
    fun `clearCache allows re-validation`() = runBlocking {
        val fake = FakeLLMClient("YES: real word")
        val v = WordValidatorAI(llmClient = fake)

        v.validate("flibbertigibbet")
        v.clearCache()
        v.validate("flibbertigibbet")

        assertEquals("LLM should be called again after cache clear", 2, fake.callCount)
    }

    // ── LLM path ──────────────────────────────────────────────────────────────

    @Test
    fun `LLM YES response marks word as valid`() = runBlocking {
        val v = WordValidatorAI(llmClient = FakeLLMClient("YES: valid English noun"))
        assertTrue(v.validate("flibbertigibbet").isValid)
    }

    @Test
    fun `LLM NO response marks word as invalid`() = runBlocking {
        val v = WordValidatorAI(llmClient = FakeLLMClient("NO: not a real word"))
        assertFalse(v.validate("xyzqwk").isValid)
    }

    @Test
    fun `LLM timeout assumes valid to not block player`() = runBlocking {
        // Empty response simulates timeout in test context
        val v = WordValidatorAI(llmClient = FakeLLMClient(""))
        assertTrue(v.validate("someunknownword").isValid)
    }

    @Test
    fun `LLM exception assumes valid`() = runBlocking {
        val v = WordValidatorAI(llmClient = ThrowingLLMClient())
        assertTrue(v.validate("someword").isValid)
    }

    // ── Offline dict covers all sequence generator pools ──────────────────────

    @Test
    fun `all easy pool words are in offline dictionary`() = runBlocking {
        val easyWords = listOf("cat","run","joy","sun","map","key","cup","fly","pen","arm")
        easyWords.forEach { word ->
            assertTrue("Expected '$word' in offline dict", validator.validate(word).isValid)
        }
    }

    @Test
    fun `all medium pool words are in offline dictionary`() = runBlocking {
        val mediumWords = listOf("blanket","journey","pillow","rocket","ladder","candle")
        mediumWords.forEach { word ->
            assertTrue("Expected '$word' in offline dict", validator.validate(word).isValid)
        }
    }

    @Test
    fun `all hard pool words are in offline dictionary`() = runBlocking {
        val hardWords = listOf("ephemeral","labyrinth","cognition","resonance","melancholy")
        hardWords.forEach { word ->
            assertTrue("Expected '$word' in offline dict", validator.validate(word).isValid)
        }
    }
}