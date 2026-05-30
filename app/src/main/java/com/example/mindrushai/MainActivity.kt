package com.example.mindrushai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mindrushai.ui.Feedback
import com.example.mindrushai.ui.FeedbackType
import com.example.mindrushai.ui.GameUiState
import com.example.mindrushai.ui.GameViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MindRushTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                GameScreen(
                    uiState   = uiState,
                    onStart   = viewModel::onStartGame,
                    onInput   = viewModel::onInputChanged,
                    onSubmit  = viewModel::onSubmit,
                    onRestart = viewModel::onRestart
                )
            }
        }
    }
}

// ── Theme ─────────────────────────────────────────────────────────────────────

@Composable
private fun MindRushTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary          = Color(0xFF7B8EF7),
            onPrimary        = Color(0xFF0D0D1A),
            secondary        = Color(0xFF5CC8A0),
            onSecondary      = Color(0xFF0D1A14),
            tertiary         = Color(0xFFF7A25B),
            background       = Color(0xFF0D0D1A),
            surface          = Color(0xFF161625),
            onBackground     = Color(0xFFE8E8F0),
            onSurface        = Color(0xFFE8E8F0),
            surfaceVariant   = Color(0xFF1E1E30),
            onSurfaceVariant = Color(0xFFB0B0C8),
            error            = Color(0xFFE07070),
            errorContainer   = Color(0xFF2A1515),
            onErrorContainer = Color(0xFFE0A0A0)
        ),
        content = content
    )
}

// ── Root screen ───────────────────────────────────────────────────────────────

@Composable
private fun GameScreen(
    uiState  : GameUiState,
    onStart  : () -> Unit,
    onInput  : (String) -> Unit,
    onSubmit : () -> Unit,
    onRestart: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester     = remember { FocusRequester() }

    LaunchedEffect(uiState.inputEnabled) {
        if (uiState.inputEnabled) {
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        } else {
            keyboardController?.hide()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeaderSection(uiState)
        StatusText(uiState.statusText)
        WordDisplayCard(
            displayedWord = uiState.displayedWord,
            wordVisible   = uiState.wordVisible,
            isLoading     = uiState.isLoading
        )
        if (uiState.inputEnabled && uiState.sequenceSize > 0) {
            SequenceProgressBar(
                typed = uiState.wordsTyped,
                total = uiState.sequenceSize
            )
        }
        FeedbackBanner(uiState.feedback)
        HintCard(uiState.hintText)
        AnimatedVisibility(
            visible = !uiState.gameOver,
            enter   = fadeIn(),
            exit    = fadeOut()
        ) {
            InputSection(
                text           = uiState.inputText,
                enabled        = uiState.inputEnabled,
                isValidating   = uiState.isValidating,
                focusRequester = focusRequester,
                onValueChange  = onInput,
                onSubmit       = onSubmit
            )
        }
        GameControlSection(
            gameOver  = uiState.gameOver,
            score     = uiState.score,
            isLoading = uiState.isLoading,
            onStart   = onStart,
            onRestart = onRestart
        )
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun HeaderSection(uiState: GameUiState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text          = "MindRush AI",
            fontSize      = 26.sp,
            fontWeight    = FontWeight.Bold,
            color         = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            StatItem(label = "Score",  value = "${uiState.score}")
            StatDivider()
            StatItem(
                label = "Words",
                value = if (uiState.sequenceSize > 0) "${uiState.sequenceSize}" else "—"
            )
            StatDivider()
            StatItem(label = "Level",  value = "${uiState.difficulty}")
            StatDivider()
            StatItem(label = "Combo",  value = "×${uiState.combo}")
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 12.dp)
    ) {
        Text(
            text       = value,
            fontSize   = 18.sp,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.primary
        )
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatDivider() {
    Box(
        Modifier
            .height(28.dp)
            .width(1.dp)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
    )
}

// ── Status text ───────────────────────────────────────────────────────────────

@Composable
private fun StatusText(text: String) {
    AnimatedContent(
        targetState  = text,
        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
        label        = "status"
    ) { label ->
        Text(
            text      = label,
            fontSize  = 14.sp,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth()
        )
    }
}

// ── Word display card ─────────────────────────────────────────────────────────

@Composable
private fun WordDisplayCard(
    displayedWord: String,
    wordVisible  : Boolean,
    isLoading    : Boolean
) {
    val scale by animateFloatAsState(
        targetValue   = if (wordVisible) 1f else 0.85f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "wordScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color       = MaterialTheme.colorScheme.primary,
                        modifier    = Modifier.size(30.dp),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text     = "AI is generating words…",
                        fontSize = 13.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            displayedWord.isNotEmpty() -> {
                Text(
                    text       = displayedWord,
                    fontSize   = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.primary,
                    modifier   = Modifier.scale(scale),
                    textAlign  = TextAlign.Center
                )
            }
            else -> {
                Text(
                    text      = "· · ·",
                    fontSize  = 22.sp,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ── Progress bar ──────────────────────────────────────────────────────────────

@Composable
private fun SequenceProgressBar(typed: Int, total: Int) {
    val progress by animateFloatAsState(
        targetValue   = if (total > 0) typed.toFloat() / total else 0f,
        animationSpec = tween(300),
        label         = "progress"
    )
    Column(
        modifier            = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text     = "Word $typed of $total",
                fontSize = 12.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text     = "${total - typed} remaining",
                fontSize = 12.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress   = { progress },
            modifier   = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color      = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

// ── Feedback banner ───────────────────────────────────────────────────────────

@Composable
private fun FeedbackBanner(feedback: Feedback?) {
    AnimatedVisibility(
        visible = feedback != null,
        enter   = slideInVertically { -it } + fadeIn(),
        exit    = slideOutVertically { -it } + fadeOut()
    ) {
        if (feedback != null) {
            val (bg, fg) = when (feedback.type) {
                FeedbackType.SUCCESS -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) to
                        MaterialTheme.colorScheme.secondary
                FeedbackType.WARNING -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)  to
                        MaterialTheme.colorScheme.tertiary
                FeedbackType.ERROR   -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)     to
                        MaterialTheme.colorScheme.error
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = feedback.message,
                    color      = fg,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign  = TextAlign.Center
                )
            }
        }
    }
}

// ── Hint card ─────────────────────────────────────────────────────────────────

@Composable
private fun HintCard(hint: String) {
    AnimatedVisibility(
        visible = hint.isNotEmpty(),
        enter   = expandVertically() + fadeIn(),
        exit    = shrinkVertically() + fadeOut()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text          = "HINT",
                fontSize      = 10.sp,
                fontWeight    = FontWeight.Bold,
                color         = MaterialTheme.colorScheme.tertiary,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text       = hint,
                fontSize   = 15.sp,
                color      = MaterialTheme.colorScheme.onSurface,
                textAlign  = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}

// ── Input section ─────────────────────────────────────────────────────────────

@Composable
private fun InputSection(
    text          : String,
    enabled       : Boolean,
    isValidating  : Boolean,
    focusRequester: FocusRequester,
    onValueChange : (String) -> Unit,
    onSubmit      : () -> Unit
) {
    Column(
        modifier            = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value         = text,
            onValueChange = onValueChange,
            enabled       = enabled,
            placeholder   = {
                Text(
                    "Type the next word…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            },
            singleLine      = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            trailingIcon    = {
                if (isValidating) CircularProgressIndicator(
                    modifier    = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color       = MaterialTheme.colorScheme.primary
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                focusedTextColor     = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor   = MaterialTheme.colorScheme.onSurface,
                cursorColor          = MaterialTheme.colorScheme.primary
            ),
            shape    = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
        )
        Button(
            onClick  = onSubmit,
            enabled  = enabled && text.isNotBlank() && !isValidating,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape  = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(text = "Submit", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Game control ──────────────────────────────────────────────────────────────

@Composable
private fun GameControlSection(
    gameOver : Boolean,
    score    : Int,
    isLoading: Boolean,
    onStart  : () -> Unit,
    onRestart: () -> Unit
) {
    AnimatedContent(
        targetState  = gameOver,
        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
        label        = "gameControl"
    ) { isOver ->
        if (isOver) {
            GameOverCard(score = score, onRestart = onRestart)
        } else {
            Button(
                onClick  = onStart,
                enabled  = !isLoading,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape  = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor   = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(text = "Start Game", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun GameOverCard(score: Int, onRestart: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(20.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text       = "Game Over",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text     = "Final score: $score",
                fontSize = 15.sp,
                color    = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onRestart,
                colors  = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor   = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Play Again", fontWeight = FontWeight.Bold)
            }
        }
    }
}