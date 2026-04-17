# MindRush AI -- Adaptive Memory Challenge Game

## Project Description

**MindRush AI** is an Android-based memory game designed to challenge
and improve the player's short‑term memory through progressively
difficult sequence reproduction tasks.

In the game, the system generates a sequence of visual elements (such as
colors or buttons), which the player must observe and memorize. After
the sequence is displayed, the player must reproduce it correctly by
tapping the corresponding elements in the same order. Each successful
round increases the difficulty of the game.

The main objective of the project is to create a simple but engaging
mobile game that incorporates **Artificial Intelligence components** to
dynamically adjust gameplay difficulty based on player performance.

------------------------------------------------------------------------

## AI Integration

The application integrates two AI-based modules that enhance gameplay.

### SequenceGeneratorAI

This module generates the sequences presented to the player. Instead of
relying purely on random generation, the AI module can generate
sequences with controlled complexity to maintain an appropriate
challenge level.

### DifficultyAdjusterAI

This module analyzes the player's performance using metrics such as: -
success rate - response time - number of mistakes

Based on these metrics, the AI dynamically adjusts the difficulty by
modifying the sequence length or complexity, ensuring that the game
remains challenging but not frustrating.

------------------------------------------------------------------------

## Project Objectives

The main goals of this project are:

-   Develop a functional Android mobile game.
-   Integrate AI components into the gameplay logic.
-   Apply modern software development practices such as:
    -   version control with Git
    -   backlog management
    -   user story definition
    -   software architecture planning
    -   automated testing
-   Demonstrate the use of AI tools throughout the software development
    lifecycle.

------------------------------------------------------------------------

## Target Platform

The application will be developed for **Android devices**, using modern
mobile development technologies. The goal is to create a responsive and
intuitive user interface that allows players to easily interact with the
game mechanics.

------------------------------------------------------------------------

## User Stories
Core Gameplay

1. As a player, I want to start a new game so that I can begin a memory challenge.
2. As a player, I want to see a generated sequence clearly so that I can memorize it.
3. As a player, I want to reproduce the sequence by tapping buttons so that I can test my memory.
4. As a player, I want to receive instant feedback if my answer is correct or wrong.
5. As a player, I want the sequence to increase in difficulty after each correct round.
6. As a player, I want to see my current score during the game.
7. As a player, I want to see a Game Over screen when I fail.

🤖 AI-related

8. As a player, I want the game difficulty to adapt based on my performance.
9. As a developer, I want an AI module that generates intelligent sequences instead of purely random ones.
10. As a developer, I want an AI module that analyzes response time and success rate.
11. As a developer, I want to store gameplay metrics for AI analysis.

📊 Persistence & UX

12. As a player, I want my best score to be saved locally.
13. As a player, I want to restart the game anytime.
14. As a player, I want a clean and intuitive interface.
15. As a developer, I want modular architecture so that AI agents can be replaced easily.

------------------------------------------------------------------------

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

**Define the foundations for the AI Agents.**

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

## Project Structure

    MindRushAI/
    │
    ├── app/
    ├── docs/
    │   ├── user_stories.md
    │   ├── backlog.md
    │   ├── AI_usage_report.md
    │   └── diagrams/
    │
    ├── README.md
    └── tests/

------------------------------------------------------------------------

## Technologies

-   Android Studio
-   Kotlin
-   Git / GitHub
-   AI-assisted development tools
