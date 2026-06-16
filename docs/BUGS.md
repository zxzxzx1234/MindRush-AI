# Known Bugs & Resolutions — MindRush AI

---

## Fixed bugs

### BUG-001 — AI agents silently falling back to 3-letter words

**Symptom**: Game always generated very short words regardless of difficulty. LLM agents appeared to not be running.

**Root cause**: `android:usesCleartextTraffic="true"` was missing from `AndroidManifest.xml`. Android blocks plain HTTP traffic by default from API 28+. LM Studio serves on `http://10.0.2.2:1234` — plain HTTP. All requests were silently dropped by the OS. `LMStudioClient` caught the exception and returned `""`, which correctly triggered the fallback pools. The fallback worked so well that there was no visible crash or error.

**Fix**: Added `android:usesCleartextTraffic="true"` inside the `<application>` tag in `AndroidManifest.xml`.

**Lesson**: A well-implemented fallback can mask infrastructure failures completely. Always verify the primary path is active (check Logcat for `[SEQUENCE_LLM]` vs `[SEQUENCE_TIMEOUT]`).

---

### BUG-002 — `GameStats` computed property in data class constructor

**Symptom**: Compilation error — `Expecting comma or ')'`, `Primary constructor of data class must only have property parameters`.

**Root cause**: `sessionAccuracy` was defined with `get() = ...` inside the data class primary constructor, which Kotlin does not allow. Computed properties in data classes must be defined in the class body, not the constructor.

**Fix**: Changed `sessionAccuracy` to a plain `val` with a default value of `0f`. The value is now computed before construction in `GameViewModel.buildStats()`.

---

### BUG-003 — `TestableGameManager : GameManager()` fails to compile

**Symptom**: `This type is final, so it cannot be extended` in `GameManagerTest.kt`.

**Root cause**: Kotlin makes all classes `final` by default. `GameManager` was never declared `open`, so subclassing was impossible.

**Fix**: Removed `TestableGameManager` entirely. `GameManagerTest` now instantiates `GameManager` directly and relies on the LLM timeout (LM Studio not running during tests) to activate the fallback pools, which is the correct behaviour to test anyway.

---

### BUG-004 — LLM timeout too short for CPU-bound models

**Symptom**: `[SEQUENCE_TIMEOUT_FALLBACK]` appearing in Logcat even with LM Studio running. Agents consistently using fallback pools.

**Root cause**: Initial timeouts (3–5 seconds) were insufficient. Small local models (3B–7B parameters) running on CPU can take 8–12 seconds to generate a short word list.

**Fix**: Increased timeouts across all agents:
- `SequenceGeneratorAI`: 6000ms → 15000ms
- `WordValidatorAI`: 4000ms → 10000ms
- `HintGeneratorAI`: 5000ms → 12000ms
- `LMStudioClient` `readTimeout`: 10s → 20s

---

### BUG-005 — Sequence always length 1-2, difficulty not increasing

**Symptom**: Game generated very short sequences that never got longer. Difficulty stayed at 1.

**Root cause**: Sequence length was `difficulty + 1`. `DifficultyAdjusterAI` had a `MIN_SAMPLES = 3` guard that prevented any adjustment until 3 rounds had been played with data. Combined, this meant the game felt stuck.

**Fix**:
- Sequence length changed to `roundsCompleted + 2` — now grows independently of difficulty, guaranteed +1 every successful round
- Removed `MIN_SAMPLES` gate from `DifficultyAdjusterAI` — now reacts after every round
- History window reduced from 8 to 5 for faster reaction

---

## Known limitations (not bugs)

- **Physical device**: `10.0.2.2` only works on the Android Emulator. On a physical device, replace with the PC's LAN IP address in `LMStudioClient.kt`.
- **Model cold start**: First request after loading a model in LM Studio can take 20–30 seconds. Subsequent requests are faster. The game will use fallback pools for the first round if the model hasn't warmed up.
- **`ScoreRepository` tests require Mockito**: The `ScoreRepositoryTest` mocks Android's `SharedPreferences`. Mockito must be added to `build.gradle.kts` test dependencies.
