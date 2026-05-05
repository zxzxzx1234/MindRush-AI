# AI Tools Usage Report -- MindRush AI

This document describes how AI tools were used throughout the development of the MindRush AI project, as required by the MDS 2026 lab grading rubric (component B, item 7).

---

## 1. Tools used

| Tool | Purpose | Where used |
|---|---|---|
| **ChatGPT (GPT-4 / GPT-4o)** | Brainstorming, user story refinement, backlog structuring | Planning phase, README writing |
| **Claude (Anthropic)** | Code review, documentation generation, diagram authoring (Mermaid), compliance auditing against the MDS rubric | Documentation, architecture diagrams, CI workflows, this report |
| **GitHub Copilot** | Inline code completion in Android Studio (Kotlin) | `GameManager.kt`, AI agent classes, LLM clients |
| **Cursor IDE** | Larger refactors and multi-file edits with AI assistance | Refactoring `AIManager` and the `LLMClient` interface |
| **LM Studio (local LLM)** | Runtime AI agent backend during gameplay | `LMStudioClient.kt` |
| **Ollama (local LLM)** | Runtime AI agent backend during gameplay | `OllamaClient.kt` |
| **OpenAI API** | Optional cloud LLM backend | `OpenAIClient.kt` |

---

## 2. Phase-by-phase usage

### 2.1 Requirements & user stories
- **Tools**: ChatGPT, Claude
- **What we did**: We described the game concept in plain language and asked the AI to propose user stories in the standard *"As a ... I want ... so that ..."* format. We then curated, deduplicated and grouped them into the three buckets visible in `docs/user_stories.md` (Core Gameplay / AI-related / Persistence & UX).
- **Result**: 15 user stories, all human-reviewed.

### 2.2 Backlog & EPIC structuring
- **Tools**: ChatGPT
- **What we did**: We pasted the user stories and asked the model to derive EPICs and tasks. The model output was reorganised manually into 10 EPICs (`docs/backlog.md`).

### 2.3 Architecture & diagrams
- **Tools**: Claude
- **What we did**: We described the existing class layout and asked Claude to produce Mermaid diagrams (component, class, sequence, state machine). All diagrams in `docs/diagrams/` were AI-generated and then reviewed against the actual source files.

### 2.4 Implementation -- game logic
- **Tools**: GitHub Copilot, Cursor
- **What we did**: Most of the boilerplate in `GameManager.kt` (state machine, score handling, input validation) was Copilot-completed line by line. Larger structural changes (extracting `AIManager`, introducing `LLMClient`) were done in Cursor with multi-file AI edits.
- **Estimated AI authorship of code**: ~60-70% of lines initially produced by AI, then reviewed and adjusted by humans.

### 2.5 Implementation -- AI agents
- **Tools**: ChatGPT (prompt design), Copilot (code completion)
- **What we did**:
  - Designed the prompts that `SequenceGeneratorAI` and `DifficultyAdjusterAI` send to the LLM by iterating with ChatGPT until the local Ollama model gave usable structured output.
  - Wrote the heuristic fallbacks with Copilot.
- **Notable issue**: Small local LLMs (e.g., 3B-parameter models in Ollama) frequently hallucinate non-numeric tokens. This is exactly the kind of behaviour the rubric expects us to handle, so the fallback path triggers whenever parsing fails.

### 2.6 LLM client integration
- **Tools**: Cursor, Claude
- **What we did**: Asked Claude to draft a minimal `LLMClient` interface compatible with LM Studio's, Ollama's and OpenAI's HTTP APIs. Cursor was then used to implement the three concrete clients in parallel.

### 2.7 Testing & evals
- **Tools**: Claude (test plan), Copilot (test code)
- **What we did**:
  - Asked Claude to propose an eval strategy for non-deterministic LLM agents (see `docs/TESTING.md`).
  - Used Copilot to expand from the existing `ExampleUnitTest` skeleton toward unit tests for `GameManager` and the heuristic fallbacks.

### 2.8 CI/CD pipeline
- **Tools**: Claude
- **What we did**: Asked Claude to author GitHub Actions workflows for an Android Gradle project (build + unit tests + lint). The workflows live in `.github/workflows/`.

### 2.9 Documentation
- **Tools**: Claude, ChatGPT
- **What we did**: README, `CONTRIBUTING.md`, `BUGS.md`, this very report and all diagram explanations were drafted with AI and human-edited.

---

## 3. What worked well

- **Mermaid diagrams from natural language**: Claude reliably produced syntactically correct Mermaid from a textual description of the architecture.
- **Boilerplate Kotlin**: Copilot was excellent for repetitive UI code and data classes.
- **Prompt iteration**: Letting ChatGPT critique its own prompt for the AI agents reduced hallucination rates noticeably on local models.

## 4. What did not work / required human correction

- **Local LLM JSON output**: Small local models often returned malformed JSON. We had to add tolerant parsing + heuristic fallback rather than trusting the model.
- **Hallucinated APIs**: Copilot occasionally suggested Android APIs that don't exist on the target SDK level; these were caught at compile time.
- **Backlog over-generation**: The first AI-generated backlog had ~25 EPICs, most of them trivial. We collapsed it manually to 10 meaningful EPICs.

## 5. Verification & ethical notes

- All AI-generated code was reviewed by a human team member before commit.
- No AI tool was given private credentials. The OpenAI API key (when used) is read from an environment variable, not committed.
- This project is original; no AI tool was prompted with someone else's project description, only with our own.

---

## 6. Summary of AI footprint

| Artifact | % AI-authored (initial draft) | % human-edited afterwards |
|---|---|---|
| User stories | 80% | 30% |
| Backlog | 70% | 50% |
| Game logic (Kotlin) | 60% | 40% |
| AI agent classes | 65% | 45% |
| LLM clients | 75% | 25% |
| Diagrams | 95% | 15% |
| README & docs | 85% | 30% |
| CI workflows | 90% | 20% |

The percentages are self-reported estimates, not measured by tooling.
