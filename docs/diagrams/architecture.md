# MindRush AI -- Component Architecture

This document describes the high-level component architecture of the MindRush AI Android application.

## High-Level Architecture

```mermaid
flowchart TB
    subgraph UI["UI Layer (Jetpack Compose)"]
        MA[MainActivity]
        SS[Start Screen]
        GS[Game Screen]
        GO[Game Over Screen]
    end

    subgraph GAME["Game Layer"]
        GM[GameManager]
        GS_STATE[GameState<br/>START / PLAYING / GAME_OVER]
    end

    subgraph AI["AI Layer"]
        AIM[AIManager<br/>Orchestrator]
        SGA[SequenceGeneratorAI<br/>Agent #1]
        DAA[DifficultyAdjusterAI<br/>Agent #2]
        AIL[AILogger]
        AID[AIDecision]
    end

    subgraph LLM["LLM Backends (pluggable)"]
        LLMC[LLMClient interface]
        LMS[LMStudioClient<br/>local]
        OLL[OllamaClient<br/>local]
        OAI[OpenAIClient<br/>cloud]
        FB[Heuristic Fallback]
    end

    subgraph PERSIST["Persistence"]
        SP[SharedPreferences<br/>best score, metrics]
    end

    MA --> SS
    MA --> GS
    MA --> GO
    GS --> GM
    GM --> GS_STATE
    GM --> AIM
    AIM --> SGA
    AIM --> DAA
    AIM --> AIL
    SGA --> AID
    DAA --> AID
    SGA --> LLMC
    DAA --> LLMC
    LLMC -.implements.-> LMS
    LLMC -.implements.-> OLL
    LLMC -.implements.-> OAI
    LLMC -.fallback.-> FB
    GM --> SP

    classDef agent fill:#ffd54f,stroke:#333,color:#000
    classDef llm fill:#90caf9,stroke:#333,color:#000
    class SGA,DAA agent
    class LMS,OLL,OAI,FB llm
```

## Layer Responsibilities

| Layer | Responsibility |
|---|---|
| **UI** | Renders screens with Jetpack Compose, captures user input. |
| **Game** | Manages game state machine, round progression, input validation, score persistence. |
| **AI** | Hosts the two AI agents, orchestrates their interaction with the game loop, logs decisions. |
| **LLM** | Pluggable LLM clients (LM Studio, Ollama, OpenAI) behind a single `LLMClient` interface, with rule-based fallback when no LLM is reachable. |
| **Persistence** | Stores best score and gameplay metrics in `SharedPreferences`. |

## Key Architectural Decisions

1. **Two cooperating AI agents**: `SequenceGeneratorAI` (decides *what* the next sequence should be) and `DifficultyAdjusterAI` (decides *how hard* it should be). Their outputs are combined by `AIManager`.
2. **LLM-agnostic design**: The agents call an abstract `LLMClient`. This lets us swap small local models (Ollama / LM Studio) without touching the agents themselves.
3. **Graceful degradation**: When no LLM backend is reachable, the agents fall back to deterministic heuristics so the game keeps working offline.
4. **Single Activity + Compose**: `MainActivity` is the only Activity; navigation between screens is handled in Compose state.
