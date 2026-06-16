package com.example.mindrushai.ai.eval

import com.example.mindrushai.ai.DifficultyAdjusterAI
import com.example.mindrushai.ai.FakeLLMClient
import com.example.mindrushai.ai.SequenceGeneratorAI
import com.example.mindrushai.ai.WordValidatorAI
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * AI Eval Harness — quality evaluations for the three AI agents.
 *
 * These are not simple pass/fail unit tests. They measure:
 *   - Output quality across multiple simulated runs
 *   - Fallback reliability when LLM is unavailable
 *   - Difficulty adjuster behaviour across realistic gameplay scenarios
 *   - Word validator accuracy on known-good and known-bad inputs
 *
 * Evals use success rate thresholds rather than exact assertions,
 * mimicking how real AI evaluation works.
 */
class AIEvalHarnessTest {

    // =========================================================================
    // EVAL 1 — SequenceGeneratorAI: fallback pool quality
    // =========================================================================

    @Test
    fun `eval - fallback pools always return correct length across all difficulties`() = runBlocking {
        val gen = SequenceGeneratorAI(llmClient = null)
        val requestedLength = 4
        var passed = 0

        for (difficulty in 1..10) {
            val result = gen.generateSequence(requestedLength, difficulty)
            if (result.size == requestedLength) passed++
        }

        val rate = passed.toFloat() / 10
        assertTrue(
            "Expected 100% correct length across difficulties, got ${rate * 100}%",
            rate >= 1.0f
        )
    }

    @Test
    fun `eval - fallback pools contain only letters no numbers or symbols`() = runBlocking {
        val gen = SequenceGeneratorAI(llmClient = null)
        var totalWords = 0
        var validWords = 0

        for (difficulty in 1..10) {
            val words = gen.generateSequence(5, difficulty)
            totalWords += words.size
            validWords += words.count { w -> w.all { it.isLetter() } }
        }

        val rate = validWords.toFloat() / totalWords
        assertTrue(
            "Expected 100% letter-only words, got ${rate * 100}%",
            rate >= 1.0f
        )
    }

    @Test
    fun `eval - fallback outputs are varied across 5 calls`() = runBlocking {
        val gen = SequenceGeneratorAI(llmClient = null)
        val results = (1..5).map { gen.generateSequence(3, 1).toSet() }
        // At least 2 of the 5 calls should return a different set
        val uniqueResults = results.map { it.sorted() }.distinct().size
        assertTrue(
            "Expected at least 2 distinct results out of 5 calls (shuffled pool), got $uniqueResults",
            uniqueResults >= 2
        )
    }

    @Test
    fun `eval - LLM response parser handles 10 different messy formats`() = runBlocking {
        val messyResponses = listOf(
            "cat,dog,sun",                           // clean
            "  cat , dog , sun  ",                   // spaces
            "Cat,DOG,SUN",                           // uppercase
            "1. cat\n2. dog\n3. sun",                // numbered
            "[cat, dog, sun]",                       // brackets
            "cat\ndog\nsun",                         // newlines
            "\"cat\",\"dog\",\"sun\"",               // quoted
            "Here are 3 words: cat, dog, sun",       // preamble
            "cat; dog; sun",                         // semicolons
            "cat - dog - sun"                        // dashes
        )

        var parsed = 0
        for (response in messyResponses) {
            val fake = FakeLLMClient(response)
            val gen = SequenceGeneratorAI(llmClient = fake)
            val result = gen.generateSequence(3, 1)
            if (result.size == 3 && result.all { it.isNotBlank() && it.all { c -> c.isLetter() } }) {
                parsed++
            }
        }

        val rate = parsed.toFloat() / messyResponses.size
        assertTrue(
            "Expected >= 80% parse success on messy responses, got ${rate * 100}% ($parsed/10)",
            rate >= 0.80f
        )
    }

    // =========================================================================
    // EVAL 2 — WordValidatorAI: offline dictionary accuracy
    // =========================================================================

    @Test
    fun `eval - offline dictionary accepts all sequence generator fallback words`() = runBlocking {
        val validator = WordValidatorAI(llmClient = null)

        val allFallbackWords = listOf(
            // easy pool sample
            "cat","dog","run","joy","sun","map","key","cup","fly","pen",
            // medium pool sample
            "blanket","journey","silver","rocket","tunnel","candle",
            // hard pool sample
            "ephemeral","labyrinth","cognition","resonance","melancholy"
        )

        var accepted = 0
        for (word in allFallbackWords) {
            val result = validator.validate(word)
            if (result.isValid) accepted++
        }

        val rate = accepted.toFloat() / allFallbackWords.size
        assertTrue(
            "Expected 100% acceptance of fallback words, got ${rate * 100}% ($accepted/${allFallbackWords.size})",
            rate >= 1.0f
        )
    }

    @Test
    fun `eval - offline dictionary rejects obvious non-words`() = runBlocking {
        val validator = WordValidatorAI(llmClient = null)

        val nonWords = listOf(
            "xzqwjk", "aaabbbccc", "qqqqqq", "zzzzzz", "mxpzr"
        )

        var rejected = 0
        for (word in nonWords) {
            val result = validator.validate(word)
            if (!result.isValid) rejected++
        }

        val rate = rejected.toFloat() / nonWords.size
        assertTrue(
            "Expected 100% rejection of non-words offline, got ${rate * 100}%",
            rate >= 1.0f
        )
    }

    @Test
    fun `eval - session cache prevents duplicate LLM calls`() = runBlocking {
        val fake = FakeLLMClient("YES: real word")
        val validator = WordValidatorAI(llmClient = fake)

        // Call with same word 3 times — LLM should only be called once
        validator.validate("flibbertigibbet")
        validator.validate("flibbertigibbet")
        validator.validate("flibbertigibbet")

        assertEquals(
            "LLM should be called exactly once, then cached",
            1,
            fake.callCount
        )
    }

    @Test
    fun `eval - single letters always accepted regardless of LLM`() = runBlocking {
        val validator = WordValidatorAI(llmClient = null)
        val singleLetters = listOf("a", "i", "b", "c", "x")

        val allAccepted = singleLetters.all { runBlocking { validator.validate(it).isValid } }
        assertTrue("All single letters should be accepted", allAccepted)
    }

    @Test
    fun `eval - LLM YES response accepted with various formats`() = runBlocking {
        val yesFormats = listOf(
            "YES: common noun",
            "Yes: it exists",
            "YES",
            "yes - valid word",
            "YES, this is a word"
        )

        var accepted = 0
        for (response in yesFormats) {
            val fake = FakeLLMClient(response)
            val validator = WordValidatorAI(llmClient = fake)
            val result = validator.validate("testword123xyz")  // not in offline dict
            if (result.isValid) accepted++
        }

        val rate = accepted.toFloat() / yesFormats.size
        assertTrue(
            "Expected >= 80% YES formats parsed correctly, got ${rate * 100}%",
            rate >= 0.80f
        )
    }

    @Test
    fun `eval - LLM NO response rejected with various formats`() = runBlocking {
        val noFormats = listOf(
            "NO: not a word",
            "No: invalid",
            "NO",
            "no - gibberish"
        )

        var rejected = 0
        for (response in noFormats) {
            val fake = FakeLLMClient(response)
            val validator = WordValidatorAI(llmClient = fake)
            val result = validator.validate("testword123xyz")
            if (!result.isValid) rejected++
        }

        val rate = rejected.toFloat() / noFormats.size
        assertTrue(
            "Expected >= 80% NO formats parsed correctly, got ${rate * 100}%",
            rate >= 0.80f
        )
    }

    // =========================================================================
    // EVAL 3 — DifficultyAdjusterAI: gameplay simulation
    // =========================================================================

    @Test
    fun `eval - skilled player simulation reaches difficulty 5 or higher`() {
        val adjuster = DifficultyAdjusterAI()
        // Simulate a skilled player: 80% success rate, fast responses
        repeat(30) { i ->
            val success = i % 5 != 0  // fail every 5th round
            adjuster.update(success, 600L)
        }
        assertTrue(
            "Skilled player should reach difficulty >= 5, got ${adjuster.difficulty}",
            adjuster.difficulty >= 5
        )
    }

    @Test
    fun `eval - struggling player simulation stays at or near difficulty 1`() {
        val adjuster = DifficultyAdjusterAI()
        // Simulate a struggling player: all failures, slow responses
        repeat(20) { adjuster.update(false, 8000L) }
        assertEquals(
            "Struggling player should stay at MIN difficulty",
            DifficultyAdjusterAI.MIN_DIFFICULTY,
            adjuster.difficulty
        )
    }

    @Test
    fun `eval - difficulty adapts after player improves`() {
        val adjuster = DifficultyAdjusterAI()

        // Phase 1: bad performance
        repeat(10) { adjuster.update(false, 7000L) }
        val afterBadPhase = adjuster.difficulty

        // Phase 2: player improves significantly
        repeat(15) { adjuster.update(true, 500L) }
        val afterGoodPhase = adjuster.difficulty

        assertTrue(
            "Difficulty should increase after improvement: bad=$afterBadPhase, good=$afterGoodPhase",
            afterGoodPhase > afterBadPhase
        )
    }

    @Test
    fun `eval - difficulty reacts within 5 rounds after consistent performance`() {
        val adjuster = DifficultyAdjusterAI()
        val initial = adjuster.difficulty

        // 5 perfect rounds
        repeat(5) { adjuster.update(true, 300L) }

        assertTrue(
            "Difficulty should have changed after 5 perfect rounds",
            adjuster.difficulty != initial
        )
    }

    @Test
    fun `eval - 50 round mixed simulation stays within valid bounds`() {
        val adjuster = DifficultyAdjusterAI()
        val random = java.util.Random(42)  // fixed seed for reproducibility

        repeat(50) {
            val success = random.nextBoolean()
            val time    = (random.nextInt(5000) + 500).toLong()
            adjuster.update(success, time)
        }

        assertTrue(
            "Difficulty must stay in [${DifficultyAdjusterAI.MIN_DIFFICULTY}..${DifficultyAdjusterAI.MAX_DIFFICULTY}]",
            adjuster.difficulty in DifficultyAdjusterAI.MIN_DIFFICULTY..DifficultyAdjusterAI.MAX_DIFFICULTY
        )
    }
}