# MindRush AI -- UML Class Diagram

This diagram captures the main classes, interfaces and their relationships in the MindRush AI codebase.

```mermaid
classDiagram
    class MainActivity {
        +onCreate(savedInstanceState: Bundle)
        -renderCurrentScreen()
    }

    class GameManager {
        -state: GameState
        -score: Int
        -bestScore: Int
        -currentSequence: List~Int~
        -playerInput: List~Int~
        +startGame()
        +submitInput(value: Int)
        +nextRound()
        +gameOver()
        +restart()
    }

    class GameState {
        <<enumeration>>
        START
        PLAYING
        GAME_OVER
    }

    class AIManager {
        -sequenceGenerator: SequenceGeneratorAI
        -difficultyAdjuster: DifficultyAdjusterAI
        -llmClient: LLMClient
        -logger: AILogger
        +generateNextSequence(metrics: PlayerMetrics): List~Int~
        +adjustDifficulty(metrics: PlayerMetrics): DifficultyLevel
        +setBackend(client: LLMClient)
    }

    class SequenceGeneratorAI {
        -llmClient: LLMClient
        +generate(length: Int, complexity: Int): List~Int~
        -fallbackHeuristic(length: Int): List~Int~
    }

    class DifficultyAdjusterAI {
        -llmClient: LLMClient
        +adjust(metrics: PlayerMetrics): DifficultyLevel
        -fallbackHeuristic(metrics: PlayerMetrics): DifficultyLevel
    }

    class AIDecision {
        +agent: String
        +input: String
        +output: String
        +reasoning: String
        +timestamp: Long
    }

    class AILogger {
        -decisions: MutableList~AIDecision~
        +log(decision: AIDecision)
        +recent(n: Int): List~AIDecision~
    }

    class LLMClient {
        <<interface>>
        +complete(prompt: String): String
        +isAvailable(): Boolean
    }

    class LMStudioClient {
        -baseUrl: String
        +complete(prompt: String): String
        +isAvailable(): Boolean
    }

    class OllamaClient {
        -baseUrl: String
        -model: String
        +complete(prompt: String): String
        +isAvailable(): Boolean
    }

    class OpenAIClient {
        -apiKey: String
        -model: String
        +complete(prompt: String): String
        +isAvailable(): Boolean
    }

    MainActivity --> GameManager : uses
    GameManager --> GameState : has
    GameManager --> AIManager : delegates AI to
    AIManager --> SequenceGeneratorAI : owns
    AIManager --> DifficultyAdjusterAI : owns
    AIManager --> AILogger : logs to
    AIManager --> LLMClient : uses
    SequenceGeneratorAI --> LLMClient : uses
    DifficultyAdjusterAI --> LLMClient : uses
    SequenceGeneratorAI ..> AIDecision : produces
    DifficultyAdjusterAI ..> AIDecision : produces
    AILogger --> AIDecision : stores
    LLMClient <|.. LMStudioClient
    LLMClient <|.. OllamaClient
    LLMClient <|.. OpenAIClient
```

## Notes

- **`LLMClient`** is the abstraction that lets the two AI agents stay independent of any specific LLM provider.
- **`AIDecision`** is a value object so each agent's output (and reasoning) can be logged uniformly through `AILogger`. This is what makes the AI behaviour explainable (EPIC 7 in the backlog).
- The dependency direction is **UI → Game → AI → LLM**, never the reverse. This keeps the AI layer testable in isolation.
