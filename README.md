# MindRush AI — Adaptive Memory Challenge Game

## Quick links (MDS rubric checklist)

| Rubric item                    | Where to find it                                                                                             |
| ------------------------------ | ------------------------------------------------------------------------------------------------------------ |
| User stories (≥10)             | [`docs/user_stories.md`](docs/user_stories.md)                             |
| Backlog                        | [`docs/backlog.md`](docs/backlog.md)                                       |
| Component architecture diagram | [`docs/diagrams/architecture.md`](docs/diagrams/architecture.md)           |
| UML class diagram              | [`docs/diagrams/class-diagram.md`](docs/diagrams/class-diagram.md)         |
| Gameplay & AI workflow         | [`docs/diagrams/gameplay-workflow.md`](docs/diagrams/gameplay-workflow.md) |
| Testing & AI evals strategy    | [`docs/TESTING.md`](docs/TESTING.md)                                       |
| Known bugs & resolutions       | [`docs/BUGS.md`](docs/BUGS.md)                                             |
| AI tools usage report          | [`docs/AI_USAGE.md`](docs/AI_USAGE.md)                                     |
| Contributing guide             | [`CONTRIBUTING.md`](CONTRIBUTING.md)                                       |
| CI/CD pipelines                | [`.github/workflows/`](.github/workflows)                                  |
| Issue templates                | [`.github/ISSUE_TEMPLATE/`](.github/ISSUE_TEMPLATE)                        |
| PR template                    | [`.github/pull_request_template.md`](.github/pull_request_template.md)     |


## Project Description

**MindRush AI** is an Android-based memory game designed to challenge and improve the player's short-term memory through progressively difficult sequence reproduction tasks.

Each round, the game generates a sequence of English words shown one at a time. The words disappear — the player must type them all back in order from memory. Each successful round adds one more word to the sequence. One wrong word ends the game.

The main objective of the project is to create a simple but engaging mobile game that incorporates **Artificial Intelligence components** to dynamically adjust gameplay difficulty based on player performance.

---

## AI Integration

The application integrates three non-deterministic AI agents coordinated through an **AIManager** that supports multiple LLM backends (LM Studio, Ollama) with local heuristic fallbacks.

### SequenceGeneratorAI

Generates English words calibrated to the current difficulty level. Instead of relying on a static list, the LLM produces varied, phonetically distinct words on every call — the same prompt yields different words each round.

Word profiles by difficulty:
- Difficulty 1–3 → short (3–4 letters), high-frequency e.g. cat, run, joy
- Difficulty 4–6 → medium (5–7 letters), everyday e.g. blanket, forest
- Difficulty 7–10 → long (8+ letters), rare/advanced e.g. ephemeral, labyrinth

### WordValidatorAI

Confirms that a word typed by the player exists in the English language. Uses a three-tier lookup:
1. Instant: single letters, session cache, offline dictionary (~600 words)
2. Instant: session invalid cache (prevents repeated LLM calls for gibberish)
3. LLM: only for genuinely unknown words — result is cached in session

On timeout, the word is assumed valid so the player is never blocked unfairly.

### HintGeneratorAI

After a wrong word, generates a short contextual memory hint. The prompt includes the full round sequence, the word's position, and its neighbours — associative context improves recall. Hints escalate in explicitness across attempts.

This cannot be done deterministically: useful memory hints require semantic understanding and creative variation.

### DifficultyAdjusterAI

Deterministic local engine — no LLM. Computes a performance score from success rate and response time, then adjusts the word profile difficulty. Reacts after every round with no minimum sample requirement.

### LLM Integration

The AI agents use interchangeable LLM backends:

- **LM Studio** — local LLM inference (recommended, free)
- **Ollama** — local LLM inference

When no LLM is available, all agents fall back to curated static pools transparently.

**Setup for LM Studio:**
1. LM Studio → Developer tab → load any model → **Start Server** (port 1234)
2. The Android emulator reaches the host via `10.0.2.2:1234`
3. `AndroidManifest.xml` must contain:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

and inside `<application>`:

```xml
android:usesCleartextTraffic="true"
```

Without `usesCleartextTraffic`, Android silently blocks HTTP and the game falls back to static pools with no error visible.

---

## Project Objectives

The main goals of this project are:

- Develop a functional Android mobile game.
- Integrate AI components into the gameplay logic.
- Apply modern software development practices such as:
  - version control with Git
  - backlog management
  - user story definition
  - software architecture planning
  - automated testing
- Demonstrate the use of AI tools throughout the software development lifecycle.

---

## Target Platform

The application is developed for **Android devices** (API 28+), using modern mobile development technologies with Jetpack Compose and MVVM architecture.

---

## User Stories

Core Gameplay

1. As a player, I want to start a new game so that I can begin a memory challenge.
2. As a player, I want to see a generated sequence clearly so that I can memorize it.
3. As a player, I want to reproduce the sequence by typing the words so that I can test my memory.
4. As a player, I want to receive instant feedback if my answer is correct or wrong.
5. As a player, I want the sequence to increase in difficulty after each correct round.
6. As a player, I want to see my current score during the game.
7. As a player, I want to see a Game Over screen when I fail.

🤖 AI-related

8. As a player, I want the game difficulty to adapt based on my performance.
9. As a developer, I want an AI module that generates intelligent word sequences instead of purely random ones.
10. As a developer, I want an AI module that validates player input against real English vocabulary.
11. As a developer, I want an AI module that generates contextual memory hints after a failure.
12. As a developer, I want an AI module that analyzes response time and success rate.
13. As a developer, I want to store gameplay metrics for AI analysis.

📊 Persistence & UX

14. As a player, I want my best score to be saved locally.
15. As a player, I want to restart the game anytime.
16. As a player, I want a clean and intuitive interface.
17. As a player, I want to see my gameplay statistics (accuracy, streak, avg response time).
18. As a developer, I want modular architecture so that AI agents can be replaced easily.

---

## Backlog

### EPIC 1 — Core Game Logic

**Implement the main gameplay mechanics of the memory challenge.**

Tasks:
- Implement game state management (START, PREPARING_ROUND, SHOWING_SEQUENCE, WAITING_INPUT, ROUND_COMPLETE, GAME_OVER)
- Implement round progression system (sequence grows by 1 each round)
- Implement sequence storage structure
- Implement player input validation
- Implement game over logic
- Implement restart game functionality

---

### EPIC 2 — AI Foundations

**Define the foundations for the AI Agents.**

Tasks:
- Define AI decision rules
- Implement pattern logic
- Support AI agent behavior
- Define LLMClient interface for interchangeable backends

---

### EPIC 3 — AI Agent: SequenceGeneratorAI

**Develop an AI agent responsible for generating intelligent word sequences.**

Tasks:
- Design agent architecture
- Generate word sequences using LLM with difficulty-calibrated prompts
- Implement phonetic contrast and semantic variety guidance in prompts
- Implement robust parser for messy LLM output
- Curate fallback word pools by difficulty level

---

### EPIC 4 — AI Agent: WordValidatorAI

**Develop an AI agent that validates player input against real English.**

Tasks:
- Implement three-tier lookup (cache → offline dict → LLM)
- Build offline dictionary covering all fallback pool words
- Implement session cache to avoid repeated LLM calls
- Handle timeout gracefully (assume valid)

---

### EPIC 5 — AI Agent: HintGeneratorAI

**Develop an AI agent that generates contextual memory hints.**

Tasks:
- Design context-aware prompt including sequence position and neighbours
- Implement progressive explicitness (3 levels based on attempt number)
- Implement local fallback hints

---

### EPIC 6 — AI Agent: DifficultyAdjusterAI

**Develop a deterministic difficulty engine.**

Tasks:
- Collect gameplay metrics (success rate, response time)
- Implement performance score formula
- Adapt word profile and display speed dynamically
- React after every round without minimum sample requirement

---

### EPIC 7 — AI Agent Communication & Orchestration

**Manage interaction between AI agents and the game system.**

Tasks:
- Define communication flow between GameManager and AI agents
- Implement AIManager orchestration
- Handle AI decision timing within game loop
- Ensure consistency between agents
- Log AI decisions for debugging (AILogger)
- Ensure modular architecture for replacing AI agents

---

### EPIC 8 — User Interface

**Develop the graphical interface of the game.**

Tasks:
- Create Start Screen
- Create Game Screen with word display card
- Create Game Over Screen with hint display
- Implement progress bar during recall phase
- Display AI-driven sequence clearly with spring animations
- Display current score, best score, difficulty, combo during gameplay
- Show visual feedback for correct/incorrect/invalid input
- Implement stats screen with accuracy ring

---

### EPIC 9 — AI Feedback & Explainability

**Provide transparency for AI decisions.**

Tasks:
- Display difficulty level changes to the player
- Show hint after wrong word (HintGeneratorAI output)
- Show streak milestones and new best score banners
- Log AI reasoning via AILogger (debug mode)

---

### EPIC 10 — Data Persistence & Analytics

**Store and analyze gameplay data.**

Tasks:
- Store gameplay metrics locally (ScoreRepository with SharedPreferences)
- Save best score across sessions
- Track total rounds, games, words correct/attempted
- Display lifetime statistics on stats screen
- Prepare data for AI analysis

---

### EPIC 11 — Testing & AI Evaluation

**Ensure reliability of both game logic and AI components.**

Tasks:
- Unit tests for game logic (GameManagerTest — 29 tests)
- Unit tests for AI modules (AIManagerTest, DifficultyAdjusterAITest, SequenceGeneratorAITest, WordValidatorAITest, HintGeneratorAITest)
- Unit tests for data layer (ScoreRepositoryTest — 17 tests with Mockito)
- AI evals with quality-threshold assertions (AIEvalHarnessTest — 15 evals)
- FakeLLMClient and ThrowingLLMClient test doubles
- Simulate different player behaviors

---

### EPIC 12 — Performance & Optimization

**Optimize AI and application performance.**

Tasks:
- Tune LLM timeouts for CPU-bound local models
- Session cache in WordValidatorAI to avoid redundant LLM calls
- Ensure smooth animations (spring, tween) and input handling
- Handle offline fallback transparently (static word pools)

---

## Project Structure

```
MindRushAI/
├── app/src/main/java/com/example/mindrushai/
│   ├── ai/
│   │   ├── llm/
│   │   │   ├── LLMClient.kt
│   │   │   ├── LMStudioClient.kt
│   │   │   └── OllamaClient.kt
│   │   ├── AIDecision.kt
│   │   ├── AILogger.kt
│   │   ├── AIManager.kt
│   │   ├── DifficultyAdjusterAI.kt
│   │   ├── HintGeneratorAI.kt
│   │   ├── SequenceGeneratorAI.kt
│   │   └── WordValidatorAI.kt
│   ├── audio/
│   │   └── SoundManager.kt
│   ├── data/
│   │   ├── GameStats.kt
│   │   └── ScoreRepository.kt
│   ├── game/
│   │   └── GameManager.kt
│   └── ui/
│       ├── theme/
│       │   ├── Color.kt
│       │   ├── MindRushTheme.kt
│       │   └── Type.kt
│       ├── GameUiState.kt
│       ├── GameViewModel.kt
│       ├── StatsScreen.kt
│       └── MainActivity.kt
├── app/src/test/java/com/example/mindrushai/
│   ├── ai/
│   │   ├── eval/
│   │   │   └── AIEvalHarnessTest.kt
│   │   ├── FakeLLMClient.kt
│   │   ├── AIManagerTest.kt
│   │   ├── DifficultyAdjusterAITest.kt
│   │   ├── HintGeneratorAITest.kt
│   │   ├── SequenceGeneratorAITest.kt
│   │   └── WordValidatorAITest.kt
│   ├── data/
│   │   └── ScoreRepositoryTest.kt
│   ├── game/
│   │   └── GameManagerTest.kt
│   └── ExampleUnitTest.kt
├── docs/
│   ├── diagrams/
│   │   ├── architecture.md
│   │   ├── class-diagram.md
│   │   └── gameplay-workflow.md
│   ├── AI_USAGE.md
│   ├── BUGS.md
│   ├── TESTING.md
│   ├── backlog.md
│   └── user_stories.md
├── .github/
│   ├── workflows/
│   │   └── ci_cd.yml
│   ├── ISSUE_TEMPLATE/
│   └── pull_request_template.md
└── README.md
```

---

## Technologies

- Android Studio
- Kotlin
- Jetpack Compose + Material3
- MVVM — ViewModel + StateFlow
- Git / GitHub
- GitHub Actions (CI/CD)
- LM Studio / Ollama (local LLM backends, free)
- OkHttp (HTTP client for LLM communication)
- JUnit 4 + Mockito-Kotlin (testing)
- AI-assisted development — see [`docs/AI_USAGE.md`](docs/AI_USAGE.md)

---

## Build & test

```bash
# Run unit tests
./gradlew testDebugUnitTest

# Run lint
./gradlew lintDebug

# Build debug APK
./gradlew assembleDebug
```

CI runs all of the above on every push and pull request to `main` and `develop`.
See [`.github/workflows/ci_cd.yml`](.github/workflows/ci_cd.yml).

Test dependencies to add in `app/build.gradle.kts`:

```kotlin
testImplementation("junit:junit:4.13.2")
testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
testImplementation("org.mockito:mockito-core:5.7.0")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
```
