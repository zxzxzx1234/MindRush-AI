# Testing & AI Evaluation Strategy

This document describes the testing approach for MindRush AI, including how we evaluate the two AI agents (which is required by the MDS rubric, item B.4: *automated tests, including evals for agents*).

---

## 1. Test layers

| Layer | Framework | Where |
|---|---|---|
| Unit tests (deterministic logic) | JUnit 4 | `app/src/test/` |
| Instrumented tests (Android-specific) | AndroidJUnit4, Espresso | `app/src/androidTest/` |
| AI agent evals (probabilistic) | JUnit 4 + custom harness | `app/src/test/.../ai/eval/` |

CI runs all layers via `./gradlew testDebugUnitTest` and `./gradlew lintDebug` (see `.github/workflows/android-test.yml`).

---

## 2. Unit tests -- game logic

Targets:

- `GameManager`
  - `startGame()` transitions state from `START` to `PLAYING`.
  - `submitInput(correct)` advances index; `submitInput(wrong)` moves to `GAME_OVER`.
  - Score increments only on a fully correct round.
  - `restart()` resets state and score but keeps best score.
  - Persistence: best score is read on init, written when beaten.

These are deterministic, hermetic tests. No network, no LLM calls.

---

## 3. Unit tests -- AI agent fallback heuristics

The fallback heuristics inside `SequenceGeneratorAI` and `DifficultyAdjusterAI` are deterministic, so they are unit-testable directly:

- `SequenceGeneratorAI.fallbackHeuristic(length)` returns a list of the requested length and only contains valid token IDs.
- `DifficultyAdjusterAI.fallbackHeuristic(metrics)` returns:
  - `EASY` when success rate < 0.4
  - `HARD` when success rate > 0.85 and average response time is low
  - `MEDIUM` otherwise

These tests run on every CI build.

---

## 4. AI agent evals (probabilistic)

LLMs are non-deterministic, so we treat their evaluation as a *statistical contract* instead of pass/fail per call.

### 4.1 Evaluation harness

For each agent we run a fixed set of inputs through the LLM-backed agent N times (default N=20) and aggregate metrics. The harness:

1. Mocks the `LLMClient` with a recorded set of plausible outputs (some valid, some malformed) -- no real network needed in CI.
2. For real-LLM runs (manual, not in CI), uses a local Ollama model.
3. Reports pass-rate per criterion.

### 4.2 Eval criteria -- SequenceGeneratorAI

| Criterion | Target |
|---|---|
| Output is parseable as `List<Int>` | ≥ 95% |
| Output length matches requested length | ≥ 90% |
| All tokens are within valid range `[0, NUM_BUTTONS)` | ≥ 95% |
| No more than one consecutive duplicate token | ≥ 80% (rest are filtered by the validator) |

### 4.3 Eval criteria -- DifficultyAdjusterAI

| Criterion | Target |
|---|---|
| Output is one of `{EASY, MEDIUM, HARD}` | 100% (with fallback) |
| Monotonicity: higher success rate ⇒ same or harder difficulty (over the test set) | ≥ 85% |
| Stability: same metrics ⇒ same difficulty across 5 calls | ≥ 80% |

### 4.4 What "passing" means

If the live (real-LLM) eval rate drops below the target, we *do not* fail CI -- we file an issue and consider:
- prompt tuning,
- model swap,
- raising the fallback threshold so the heuristic kicks in more often.

The CI eval (mocked LLM) is deterministic and is a hard pass/fail.

---

## 5. Manual / instrumented tests

- Smoke test: install debug APK, play 3 rounds, verify score increments and Game Over screen renders.
- Backend swap: switch `AIManager` between `LMStudioClient`, `OllamaClient`, `OpenAIClient`, fallback. The game must remain playable in all four configurations.

---

## 6. Roadmap (what is not yet automated)

- [ ] Espresso UI tests for the three screens.
- [ ] Property-based tests for `GameManager` round transitions.
- [ ] Snapshot tests for AI prompts (so we notice when prompt drifts).
- [ ] Recording a longer eval set (~200 inputs per agent) for nightly runs.
