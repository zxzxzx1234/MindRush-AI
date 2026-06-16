# AI Tools Usage Report — MindRush AI

This document describes how AI tools were used throughout the development of MindRush AI, as required by the MDS 2025-2026 lab grading rubric (component B, item 7).

---

## 1. Tool used

The single AI tool used across the entire project was **Claude (Anthropic)**, accessed via the claude.ai web interface.

All phases of development — from architecture design to final debugging — were carried out in a single long-running conversation with Claude. The project's source files were uploaded at each session so Claude had the full current state of the codebase.

---

## 2. How it was used

### 2.1 Architecture & design

Claude proposed the initial package structure (`ai/`, `game/`, `ui/`, `data/`, `audio/`) and the separation between deterministic logic (`DifficultyAdjusterAI`) and non-deterministic LLM agents (`SequenceGeneratorAI`, `WordValidatorAI`, `HintGeneratorAI`). The MVVM architecture with `GameViewModel` and `StateFlow` was also suggested and implemented by Claude.

### 2.2 AI agent implementation

All three AI agents were implemented iteratively with Claude:

- **SequenceGeneratorAI** — multiple prompt iterations to improve word quality, add phonetic contrast guidance, and make parsing robust against messy LLM output (numbered lists, brackets, preambles)
- **WordValidatorAI** — three-tier cache design (session cache → offline dictionary → LLM) to minimise latency during gameplay
- **HintGeneratorAI** — context-aware prompts that include the full round sequence and word position, so hints reference neighbouring words

### 2.3 Debugging

The most significant debugging session involved the AI agents not responding — the game was silently falling back to static word pools. Claude identified the root cause: `android:usesCleartextTraffic="true"` was missing from `AndroidManifest.xml`. Android blocks HTTP traffic by default from API 28+, and LM Studio runs on plain HTTP. There was no crash, no exception in Logcat — the fallback system was working correctly but hiding the infrastructure failure.

### 2.4 Feature additions

Claude added several features beyond the initial scope:
- `ScoreRepository` (SharedPreferences persistence)
- `SoundManager` (ToneGenerator audio feedback)
- `StatsScreen` with animated accuracy ring (Canvas drawing)
- Streak milestones and new-best-score celebration banners
- `GamePhase` enum replacing scattered boolean state flags

### 2.5 Testing

Claude wrote all unit tests and eval harnesses:
- `DifficultyAdjusterAITest` — 11 tests for the deterministic engine
- `SequenceGeneratorAITest` — 15 tests including parser robustness on 5 messy formats
- `AIManagerTest` — 17 tests covering all three agent delegation paths
- `WordValidatorAITest` — 22 tests including cache behaviour and LLM integration
- `HintGeneratorAITest` — 14 tests covering progressive explicitness and context
- `GameManagerTest` — 29 tests for the full state machine
- `ScoreRepositoryTest` — 17 tests using Mockito-mocked SharedPreferences
- `AIEvalHarnessTest` — 15 quality-threshold evals (not pass/fail — success rate assertions)

Total: **140 tests** across 8 test files.

A `FakeLLMClient` and `ThrowingLLMClient` were introduced as test doubles to allow deterministic testing of LLM-dependent code paths.

### 2.6 CI/CD pipeline

Claude authored the GitHub Actions workflow (`.github/workflows/ci_cd.yml`) with four jobs: unit tests, lint, debug APK build, and release APK build (main branch only).

### 2.7 Documentation

README, `TESTING.md`, `BUGS.md`, and this report were all written by Claude based on the actual state of the project.

---

## 3. What worked well

- **Iterative file-by-file development**: uploading the current source file and asking for improvements worked reliably — Claude always returned complete, compilable files
- **Debugging from symptoms**: describing observable behaviour ("game always generates 3-letter words") was enough for Claude to identify the root cause without seeing Logcat
- **Test generation**: Claude produced tests that matched the actual API signatures with zero manual correction needed (except the `GameManager` inheritance issue — see below)

---

## 4. What required correction

- **`GameManager` subclassing**: Claude initially generated a `TestableGameManager : GameManager()` test helper. Kotlin makes classes final by default, so this failed to compile. Claude corrected it on the next iteration by removing the subclass and testing `GameManager` directly
- **`GameStats` computed property**: `sessionAccuracy` was initially defined as a `get()` inside a data class constructor, which Kotlin does not allow. Fixed by pre-computing it before construction
- **Timeouts**: Initial LLM timeouts (3–5 seconds) were too short for small CPU-bound models. Increased to 10–15 seconds after observing fallback activation in Logcat

---

## 5. Summary

| Artifact | Initially written by Claude | Human changes |
|---|---|---|
| Source code (Kotlin) | ~90% | Variable renaming, package adjustments, manifest edits |
| Unit tests | 100% | One structural fix (GameManager inheritance) |
| CI/CD workflow | 100% | None |
| Documentation | 100% | None |
| AndroidManifest.xml | 0% | Added `INTERNET` permission and `usesCleartextTraffic` manually |

The percentages are self-reported estimates. All AI-generated code was reviewed before commit.
