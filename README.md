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

## Planned Features

The planned features of the application include:

-   interactive memory gameplay
-   dynamic difficulty adjustment
-   score tracking system
-   best score persistence
-   simple and intuitive user interface
-   AI-assisted gameplay balancing

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

## Backlog

*EPIC 1 – Core Game Logic*
-Implement main game screen

-Implement sequence display mechanism

-Implement user input validation

-Implement score system

*EPIC 2 – AI Integration*

-Implement SequenceGeneratorAI module

-Implement DifficultyAdjusterAI module

-Define AI-agent communication interface

-Log player performance metrics

-Adjust sequence length dynamically

*EPIC 3 – UI & UX*

-Create start screen

-Create game screen layout

-Create game over screen

-Add animations for sequence display

-Add visual feedback for correct/incorrect input


*EPIC 4 – Persistence*

-Implement local storage for best score

-Store gameplay statistics

*EPIC 5 – Testing & Quality*

-Unit tests for game logic

-Unit tests for AI modules

-Manual UI testing

-Bug reporting workflow

Implement game over logic
------------------------------------------------------------------------

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
