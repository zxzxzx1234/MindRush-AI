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

    var score by remember { mutableStateOf(0) }
    var gameOver by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(text = "Score: $score")

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            gameManager.startGame()
            score = 0
            gameOver = false
        }) {
            Text("Start")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            (0..3).forEach { index ->
                Button(
                    onClick = {
                        val result = gameManager.addPlayerInput(index, 1000)
                        score = gameManager.score

                        if (!result) {
                            gameOver = true
                        }
                    },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text("$index")
                }
            }
        }

        if (gameOver) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Game Over")
        }
    }
}