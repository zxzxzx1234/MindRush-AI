# Backlog

------------------------------------------------------------------------

## EPIC 1 -- Core Game Logic

**Implement the main gameplay mechanics of the memory challenge.**

### Tasks:

-   Implement game state management (START, PLAYING, GAME_OVER)
-   Implement round progression system
-   Implement sequence storage structure
-   Implement player input validation
-   Implement game over logic
-   Implement restart game functionality

------------------------------------------------------------------------

## EPIC 2 –- AI Foundations

### Tasks:

-   Define AI decision rules
-   Implement pattern logic
-   Support AI agent behavior
------------------------------------------------------------------------

## EPIC 3 -- AI Agent: SequenceGeneratorAI

**Develop an AI agent responsible for generating intelligent
sequences.**

### Tasks:

-   Design agent architecture
-   Generate sequences using rule-based AI logic
-   Implement configurable generation rules
-   Ensure increasing complexity using AI reasoning
-   Validate generated sequences

------------------------------------------------------------------------

## EPIC 4 -- AI Agent: DifficultyAdjusterAI

**Develop an AI agent that dynamically adjusts difficulty.**

### Tasks:

-   Collect gameplay metrics (success rate, time, mistakes)
-   Analyze player metrics locally
-   Implement heuristic-based difficulty adjustment
-   Adapt sequence length and complexity dynamically
-   Continuously refine AI decision rules

------------------------------------------------------------------------

## EPIC 5 -- AI Agent Communication & Orchestration

**Manage interaction between AI agents and the game system.**

### Tasks:

-   Define communication flow between GameManager and AI agents
-   Implement agent orchestration logic
-   Handle AI decision timing within game loop
-   Ensure consistency between agents
-   Log AI decisions for debugging
-   Ensure modular architecture for replacing AI agents

------------------------------------------------------------------------

## EPIC 6 -- User Interface

**Develop the graphical interface of the game.**

### Tasks:

-   Create Start Screen
-   Create Game Screen
-   Create Game Over Screen
-   Implement interactive buttons
-   Display AI-driven sequence clearly
-   Display current score during gameplay
-   Show visual feedback for correct/incorrect input

------------------------------------------------------------------------

## EPIC 7 -- AI Feedback & Explainability

**Provide transparency for AI decisions.**

### Tasks:

-   Display difficulty level changes to the player
-   Show simple explanations for difficulty adjustments
-   Log AI reasoning (optional debug mode)
-   Improve user trust in AI behavior

------------------------------------------------------------------------

## EPIC 8 -- Data Persistence & Analytics

**Store and analyze gameplay data.**

### Tasks:

-   Store gameplay metrics locally
-   Save AI decisions history
-   Save best score
-   Load stored data at app start
-   Prepare data for AI analysis

------------------------------------------------------------------------

## EPIC 9 -- Testing & AI Evaluation

**Ensure reliability of both game logic and AI components.**

### Tasks:

-   Unit tests for game logic
-   Unit tests for AI modules
-   Simulate different player behaviors
-   Evaluate AI performance and adjust decision rules

------------------------------------------------------------------------

## EPIC 10 -- Performance & Optimization

**Optimize AI and application performance.**

### Tasks:

-   Optimize AI decision performance
-   Optimize reuse of computed sequences when appropriate
-   Ensure smooth animations and input handling
-   Ensure smooth gameplay experience
-   Handle offline fallback (basic random mode)
