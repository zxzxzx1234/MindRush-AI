package com.example.mindrushai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mindrushai.game.GameManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val gameManager = GameManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GameScreen(gameManager)
        }
    }
}

@Composable
fun GameScreen(gameManager: GameManager) {

    val scope = rememberCoroutineScope()

    var score by remember { mutableStateOf(0) }
    var highlightedIndex by remember { mutableStateOf<Int?>(null) }
    var inputEnabled by remember { mutableStateOf(false) }
    var inputStartTime by remember { mutableStateOf(0L) }
    var animationTrigger by remember { mutableStateOf(0) }
    var gameOver by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Press Start") }

    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error
    )

    LaunchedEffect(animationTrigger) {

        if (gameManager.gameState != GameManager.GameState.SHOWING_SEQUENCE) return@LaunchedEffect

        inputEnabled = false
        statusText = "Get ready..."

        delay(800)

        statusText = "Watch carefully"

        delay(400)

        val sequence = gameManager.currentSequence

        for (i in sequence.indices) {

            val value = sequence[i]

            highlightedIndex = value
            delay(650)

            highlightedIndex = null
            delay(250)

            val nextSame =
                i < sequence.lastIndex && sequence[i + 1] == value

            if (nextSame) {
                delay(450)
            }
        }

        highlightedIndex = null

        delay(600)

        gameManager.startInputPhase()

        statusText = "Your turn"
        inputEnabled = true
        inputStartTime = System.currentTimeMillis()
    }

    suspend fun startNextRound() {
        statusText = "Nice! Next round..."
        inputEnabled = false
        delay(1200)
        animationTrigger++
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("MindRush AI", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(12.dp))

        Text("Score: $score", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(8.dp))

        Text("Difficulty: ${gameManager.difficulty}")

        Spacer(modifier = Modifier.height(12.dp))

        Text(statusText)

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                gameManager.startGame()
                score = 0
                gameOver = false
                highlightedIndex = null
                statusText = "Starting..."
                animationTrigger++
            }
        ) {
            Text("Start Game")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Column {
            for (row in 0..1) {
                Row {
                    for (col in 0..1) {

                        val index = row * 2 + col
                        val isHighlighted = highlightedIndex == index

                        Button(
                            onClick = {

                                if (!inputEnabled || gameOver) return@Button

                                val responseTime =
                                    System.currentTimeMillis() - inputStartTime

                                val result =
                                    gameManager.addPlayerInput(index, responseTime)

                                score = gameManager.score
                                highlightedIndex = index

                                if (!result) {
                                    statusText = "Wrong!"
                                    gameOver = true
                                    inputEnabled = false
                                } else if (gameManager.gameState == GameManager.GameState.SHOWING_SEQUENCE) {

                                    scope.launch {
                                        startNextRound()
                                    }
                                }

                                inputStartTime = System.currentTimeMillis()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isHighlighted) {
                                    colors[index]
                                } else {
                                    colors[index].copy(alpha = 0.4f)
                                }
                            ),
                            modifier = Modifier
                                .padding(10.dp)
                                .size(if (isHighlighted) 115.dp else 100.dp)
                        ) {}
                    }
                }
            }
        }

        if (gameOver) {
            Spacer(modifier = Modifier.height(24.dp))

            Text("Game Over")

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = {
                gameManager.resetGame()
                score = 0
                gameOver = false
                inputEnabled = false
                highlightedIndex = null
                statusText = "Press Start"
            }) {
                Text("Restart")
            }
        }
    }
}