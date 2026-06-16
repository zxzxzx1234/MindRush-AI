# Testing Strategy — MindRush AI

---

## Overview

The project has **107 unit tests** across 7 test files, divided into two categories:

- **Unit tests** — verify individual classes in isolation, deterministic pass/fail
- **AI evals** — verify agent quality with success-rate thresholds, mimicking how real LLM evaluation works

All tests run without a device or emulator (JVM-only, no Android runtime required).

---

## Test files

### `DifficultyAdjusterAITest` — 11 tests

Covers the deterministic difficulty engine:
- Initial state is `MIN_DIFFICULTY`
- `reset()` restores initial state
- Consistent success + fast responses increases difficulty
- Consistent failure + slow responses decreases difficulty
- Difficulty is bounded to `[1..10]`
- `snapshot()` returns accurate metrics
- Performance score thresholds match expected `THRESHOLD_UP` / `THRESHOLD_DOWN` values

### `SequenceGeneratorAITest` — 15 tests

Covers fallback pools and LLM integration:
- Correct length returned at all difficulties
- No duplicates in output
- Difficulty 1–3 returns short words, 4–6 medium, 7–10 long
- `FakeLLMClient` used to inject controlled responses
- Parser handles: clean CSV, spaces, uppercase, numbered lists, brackets, extra text

### `WordValidatorAITest` — 22 tests

Covers all three validation tiers:
- Tier 1 (instant): single letters, offline dict, trim/lowercase
- Tier 2 (cache): `FakeLLMClient.callCount` confirms LLM called once, then cached
- Tier 3 (LLM): YES/NO parsing, timeout → assumed valid, exception → assumed valid
- All SequenceGeneratorAI fallback pool words are accepted offline

### `HintGeneratorAITest` — 14 tests

Covers fallback and LLM integration:
- Attempt 1: generic hint for short/medium/long words
- Attempt 2: reveals first letter and length
- Attempt 3+: explicit hint with letter and count
- LLM response: used directly, quotes stripped, first line taken
- Empty LLM response / exception: falls back to local hint

### `GameManagerTest` — 23 tests

Covers the full game state machine:
- Initial state `START`, score 0, empty sequence
- `startGame()` → `SHOWING_SEQUENCE`, 2-word sequence
- `startInputPhase()` transitions state correctly
- Correct words: `CORRECT`, `ROUND_COMPLETE`, score/roundsCompleted increments
- After round 1: second sequence has 3 words
- Wrong real English word: `WRONG_WORD` + `GAME_OVER`
- Gibberish: `INVALID_WORD`, state stays `WAITING_INPUT`
- `resetGame()` clears all state

### `AIEvalHarnessTest` — 15 evals

Quality thresholds instead of exact assertions:
- Fallback pools: 100% correct length across all 10 difficulty levels
- Parser: ≥ 80% success rate on 10 different messy LLM output formats
- Offline dictionary: 100% acceptance of all fallback pool words
- Cache: `callCount == 1` after 3 identical validation requests
- YES/NO formats: ≥ 80% parsed correctly across format variations
- Difficulty simulation: skilled player reaches difficulty ≥ 5 after 30 rounds
- Struggling player simulation stays at `MIN_DIFFICULTY`
- 50-round random simulation stays within `[1..10]`

### `ExampleUnitTest` — 7 tests
Basic sanity checks for the project structure and environment.

---

## Running tests

```bash
# All unit tests
./gradlew testDebugUnitTest

# HTML report
open app/build/reports/tests/testDebugUnitTest/index.html
```

---

## Test doubles

`FakeLLMClient` and `ThrowingLLMClient` live in `test/java/com/example/mindrushai/ai/FakeLLMClient.kt`:

- `FakeLLMClient(response)` — returns a fixed string, tracks `callCount`
- `ThrowingLLMClient` — always throws `RuntimeException`, used to verify fallback paths

---

## Dependencies

Add to `app/build.gradle.kts`:

```kotlin
testImplementation("junit:junit:4.13.2")
testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
testImplementation("org.mockito:mockito-core:5.7.0")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
```
