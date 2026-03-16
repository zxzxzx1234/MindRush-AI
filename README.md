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

## EPIC 1 -- Core Game Logic

Implement the main gameplay mechanics of the memory challenge.

Tasks:
- Implement game state management (START, PLAYING, GAME_OVER)
- Implement round progression system 
- Implement sequence storage structure 
- Implement player input validation 
- Implement game over logic

------------------------------------------------------------------------

## EPIC 2 -- Sequence Generation

Responsible for generating the sequences that the player must memorize.

Tasks: 
- Implement sequence generation algorithm 
- Create SequenceGeneratorAI module 
- Generate sequences based on difficulty level 
- Ensure sequences increase progressively

------------------------------------------------------------------------

## EPIC 3 -- Adaptive Difficulty System

Introduce AI-based difficulty adjustment.

Tasks: 
- Implement DifficultyAdjusterAI module 
- Track player success rate 
- Track player response time 
- Adjust sequence length dynamically 
- Adjust difficulty based on performance

------------------------------------------------------------------------

## EPIC 4 -- User Interface

Develop the graphical interface of the game.

Tasks:
- Create Start Screen 
- Create Game Screen 
- Create Game Over Screen 
- Implement interactive buttons for player input 
- Implement visual feedback for correct/incorrect input

------------------------------------------------------------------------

## EPIC 5 -- Score and Progress Tracking

Track player performance and progression.

Tasks:
- Implement score calculation system 
- Display current score during gameplay 
- Save best score locally 
- Load best score when the application starts

------------------------------------------------------------------------

## EPIC 6 -- Animations and Visual Feedback

Improve user experience with visual effects.

Tasks: 
- Implement sequence highlight animation 
- Add visual feedback for player input 
- Add transition between screens 
- Add simple UI animations

------------------------------------------------------------------------

## EPIC 7 -- Audio System

Add sound effects to enhance gameplay.

Tasks:
- Implement sound manager 
- Add sound effect for button press 
- Add sound effect for correct sequence 
- Add sound effect for incorrect input 
- Add background music (optional)

------------------------------------------------------------------------

## EPIC 8 -- Data Persistence

Store player data locally.

Tasks:
- Implement local storage system 
- Save best score 
- Save gameplay statistics 
- Load stored data when the game starts

------------------------------------------------------------------------

## EPIC 9 -- Testing and Quality Assurance

Ensure reliability and correctness of the application.

Tasks:
- Write unit tests for game logic 
- Write unit tests for AI modules 
- Perform manual gameplay testing 
- Fix gameplay bugs 
- Document testing results

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
