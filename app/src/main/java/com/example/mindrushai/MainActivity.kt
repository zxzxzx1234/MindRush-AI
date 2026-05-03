package com.example.mindrushai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
    var pressedIndex by remember { mutableStateOf<Int?>(null) }
    var inputEnabled by remember { mutableStateOf(false) }
    var inputStartTime by remember { mutableStateOf(0L) }
    var animationTrigger by remember { mutableStateOf(0) }
    var gameOver by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Press Start") }
    var combo by remember { mutableStateOf(0) }

    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error
    )

    fun sequenceSpeed(): Long {
        return when (gameManager.difficulty) {
            in 1..2 -> 700L
            in 3..4 -> 600L
            in 5..6 -> 500L
            in 7..8 -> 420L
            else -> 350L
        }
    }

    LaunchedEffect(animationTrigger) {

        if (gameManager.gameState !=
            GameManager.GameState.SHOWING_SEQUENCE
        ) {
            return@LaunchedEffect
        }

        inputEnabled = false
        highlightedIndex = null

        statusText = "Get Ready..."
        delay(900)

        statusText = "Watch Carefully"
        delay(600)

        val speed = sequenceSpeed()
        val pause = speed / 3

        val sequence = gameManager.currentSequence

        for (i in sequence.indices) {

            val value = sequence[i]

            highlightedIndex = value
            delay(speed)

            highlightedIndex = null
            delay(pause)

            val nextSame =
                i < sequence.lastIndex &&
                        sequence[i + 1] == value

            if (nextSame) {
                delay(250)
            }
        }

        delay(500)

        gameManager.startInputPhase()

        statusText = "Your Turn"
        inputEnabled = true
        inputStartTime = System.currentTimeMillis()
    }

    suspend fun startNextRound() {
        inputEnabled = false
        statusText = "Great! Next Round..."
        delay(1200)
        animationTrigger++
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "MindRush AI",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Score: $score",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "Difficulty: ${gameManager.difficulty}",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "Combo: $combo",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(text = statusText)

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                gameManager.startGame()
                score = 0
                combo = 0
                gameOver = false
                highlightedIndex = null
                pressedIndex = null
                statusText = "Starting..."
                animationTrigger++
            }
        ) {
            Text("Start Game")
        }

        Spacer(modifier = Modifier.height(40.dp))

        Column {
            for (row in 0..1) {
                Row {
                    for (col in 0..1) {

                        val index = row * 2 + col

                        val active =
                            highlightedIndex == index ||
                                    pressedIndex == index

                        Button(
                            onClick = {

                                if (!inputEnabled || gameOver) {
                                    return@Button
                                }

                                pressedIndex = index

                                scope.launch {
                                    delay(120)
                                    pressedIndex = null
                                }

                                val responseTime =
                                    System.currentTimeMillis() -
                                            inputStartTime

                                val result =
                                    gameManager.addPlayerInput(
                                        index,
                                        responseTime
                                    )

                                score = gameManager.score

                                if (!result) {
                                    statusText = "Wrong Sequence!"
                                    gameOver = true
                                    combo = 0
                                    inputEnabled = false
                                } else if (
                                    gameManager.gameState ==
                                    GameManager.GameState.SHOWING_SEQUENCE
                                ) {
                                    combo++

                                    scope.launch {
                                        startNextRound()
                                    }
                                }

                                inputStartTime =
                                    System.currentTimeMillis()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (active) {
                                    colors[index]
                                } else {
                                    colors[index].copy(alpha = 0.35f)
                                }
                            ),
                            modifier = Modifier
                                .padding(10.dp)
                                .size(
                                    if (active) 118.dp
                                    else 100.dp
                                )
                        ) {}
                    }
                }
            }
        }

        if (gameOver) {

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "Game Over",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Final Score: $score")

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    gameManager.resetGame()

                    score = 0
                    combo = 0
                    gameOver = false
                    inputEnabled = false
                    highlightedIndex = null
                    pressedIndex = null
                    statusText = "Press Start"
                }
            ) {
                Text("Restart")
            }
        }
    }
}