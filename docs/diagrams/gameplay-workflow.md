# MindRush AI -- Gameplay & AI Workflow

Sequence and state diagrams showing how a single round of MindRush AI plays out, including the interaction with the two AI agents.

## Game State Machine

```mermaid
stateDiagram-v2
    [*] --> START
    START --> PLAYING : user taps "Start"
    PLAYING --> PLAYING : correct input / next round
    PLAYING --> GAME_OVER : wrong input
    GAME_OVER --> PLAYING : user taps "Restart"
    GAME_OVER --> [*] : user exits
```

## Round Sequence (with AI agents)

```mermaid
sequenceDiagram
    autonumber
    actor P as Player
    participant UI as UI (Compose)
    participant GM as GameManager
    participant AIM as AIManager
    participant SGA as SequenceGeneratorAI
    participant DAA as DifficultyAdjusterAI
    participant LLM as LLMClient

    P->>UI: tap "Start"
    UI->>GM: startGame()
    GM->>AIM: requestNextRound(metrics)
    AIM->>DAA: adjust(metrics)
    DAA->>LLM: complete(prompt)
    alt LLM available
        LLM-->>DAA: difficulty level
    else LLM unavailable
        DAA-->>DAA: fallback heuristic
    end
    DAA-->>AIM: DifficultyLevel
    AIM->>SGA: generate(length, complexity)
    SGA->>LLM: complete(prompt)
    alt LLM available
        LLM-->>SGA: sequence
    else LLM unavailable
        SGA-->>SGA: fallback heuristic
    end
    SGA-->>AIM: List<Int>
    AIM-->>GM: sequence + difficulty
    GM->>UI: display sequence
    UI->>P: show sequence
    P->>UI: tap inputs
    UI->>GM: submitInput(value)
    alt input matches
        GM->>UI: feedback OK
        GM->>GM: nextRound()
    else input wrong
        GM->>UI: feedback FAIL
        GM->>GM: gameOver()
        GM->>UI: show GAME_OVER
    end
```

## AI Decision Pipeline (per round)

```mermaid
flowchart LR
    M[Player Metrics<br/>success rate, response time, mistakes] --> DAA[DifficultyAdjusterAI]
    DAA -->|DifficultyLevel<br/>EASY / MEDIUM / HARD| SGA[SequenceGeneratorAI]
    SGA -->|sequence: List Int| GM[GameManager]
    DAA -.log.-> L[(AILogger)]
    SGA -.log.-> L
```

## Why two agents?

Splitting the AI into two cooperating agents matches the backlog requirement (EPIC 3 + EPIC 4) and gives us:

- **Separation of concerns**: one agent decides *how hard*, the other decides *what*.
- **Independent evaluation**: each agent can be eval-tested separately (see `docs/TESTING.md`).
- **Pluggability**: either agent can be swapped (e.g., heuristic-only vs LLM-backed) without touching the other.
