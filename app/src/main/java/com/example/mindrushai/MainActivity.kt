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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mindrushai.ui.*
import com.example.mindrushai.ui.theme.MindRushTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MindRushTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                // Navigate between game and stats screens
                if (state.showStatsScreen) {
                    StatsScreen(
                        stats    = viewModel.buildStats(),
                        onDismiss = viewModel::onDismissStats
                    )
                } else {
                    GameScreen(
                        state     = state,
                        onStart   = viewModel::onStartGame,
                        onInput   = viewModel::onInputChanged,
                        onSubmit  = viewModel::onSubmit,
                        onRestart = viewModel::onRestart,
                        onStats   = viewModel::onShowStats
                    )
                }
            }
        }
    }
}

// ── Root screen ───────────────────────────────────────────────────────────────

@Composable
private fun GameScreen(
    state    : GameUiState,
    onStart  : () -> Unit,
    onInput  : (String) -> Unit,
    onSubmit : () -> Unit,
    onRestart: () -> Unit,
    onStats  : () -> Unit
) {
    val keyboard       = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(state.inputEnabled) {
        if (state.inputEnabled) {
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        } else {
            keyboard?.hide()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeaderSection(state, onStats)
        StatusLabel(state.statusText)
        WordCard(word = state.displayedWord, visible = state.wordVisible, phase = state.phase)

        // Progress bar shown during recall phase
        if (state.phase == GamePhase.RECALL && state.sequenceSize > 0) {
            ProgressRow(typed = state.wordsTyped, total = state.sequenceSize)
        }

        // Streak milestone banner
        StreakBanner(state.streakMilestone)

        // New best score banner
        NewBestBanner(state.isNewBestScore)

        FeedbackBanner(state.feedback)
        HintCard(state.hintText)

        AnimatedVisibility(
            visible = state.phase != GamePhase.GAME_OVER,
            enter   = fadeIn(),
            exit    = fadeOut()
        ) {
            InputRow(
                text           = state.inputText,
                enabled        = state.inputEnabled,
                validating     = state.isValidating,
                focusRequester = focusRequester,
                onChange       = onInput,
                onSubmit       = onSubmit
            )
        }

        ControlSection(
            phase     = state.phase,
            score     = state.score,
            bestScore = state.bestScore,
            onStart   = onStart,
            onRestart = onRestart
        )
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun HeaderSection(state: GameUiState, onStats: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text       = "MindRush AI",
                style      = MaterialTheme.typography.headlineLarge,
                color      = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onStats) {
                Text(
                    text  = "📊",
                    fontSize = 20.sp
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            StatCell("Score",  "${state.score}",   Modifier.weight(1f))
            StatDivider()
            StatCell("Best",   "${state.bestScore}", Modifier.weight(1f))
            StatDivider()
            StatCell("Words",  if (state.sequenceSize > 0) "${state.sequenceSize}" else "—", Modifier.weight(1f))
            StatDivider()
            StatCell("Level",  "${state.difficulty}", Modifier.weight(1f))
            StatDivider()
            StatCell("Combo",  "×${state.combo}",  Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text       = value,
            style      = MaterialTheme.typography.titleLarge,
            color      = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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

// ── Status label ──────────────────────────────────────────────────────────────

@Composable
private fun StatusLabel(text: String) {
    AnimatedContent(
        targetState  = text,
        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
        label        = "status"
    ) { t ->
        Text(
            text      = t,
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth()
        )
    }
}

// ── Word display card ─────────────────────────────────────────────────────────

@Composable
private fun WordCard(word: String, visible: Boolean, phase: GamePhase) {
    val scale by animateFloatAsState(
        targetValue   = if (visible) 1f else 0.82f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "wordScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(144.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            phase == GamePhase.LOADING -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color       = MaterialTheme.colorScheme.primary,
                        modifier    = Modifier.size(28.dp),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text  = "AI is generating words…",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            word.isNotEmpty() -> {
                Text(
                    text       = word,
                    fontSize   = 46.sp,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.primary,
                    modifier   = Modifier.scale(scale),
                    textAlign  = TextAlign.Center
                )
            }
            else -> {
                Text(
                    text  = if (phase == GamePhase.RECALL) "Type the next word ↓" else "· · ·",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}

// ── Progress row ──────────────────────────────────────────────────────────────

@Composable
private fun ProgressRow(typed: Int, total: Int) {
    val progress by animateFloatAsState(
        targetValue   = if (total > 0) typed.toFloat() / total else 0f,
        animationSpec = tween(280),
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
                text  = "Word $typed of $total",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text  = "${total - typed} remaining",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress   = { progress },
            modifier   = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color      = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

// ── Streak milestone banner ───────────────────────────────────────────────────

@Composable
private fun StreakBanner(milestone: Int) {
    AnimatedVisibility(
        visible      = milestone > 0,
        enter        = slideInVertically { -it } + fadeIn(),
        exit         = slideOutVertically { -it } + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = "🔥  $milestone round streak!",
                style      = MaterialTheme.typography.labelLarge,
                color      = MaterialTheme.colorScheme.tertiary,
                textAlign  = TextAlign.Center
            )
        }
    }
}

// ── New best score banner ─────────────────────────────────────────────────────

@Composable
private fun NewBestBanner(show: Boolean) {
    AnimatedVisibility(
        visible = show,
        enter   = slideInVertically { -it } + fadeIn(),
        exit    = slideOutVertically { -it } + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text      = "🏆  New personal best!",
                style     = MaterialTheme.typography.labelLarge,
                color     = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
        }
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
        feedback?.let { fb ->
            val (bg, fg) = when (fb.type) {
                FeedbackType.SUCCESS ->
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f) to
                            MaterialTheme.colorScheme.secondary
                FeedbackType.WARNING ->
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f) to
                            MaterialTheme.colorScheme.tertiary
                FeedbackType.ERROR   ->
                    MaterialTheme.colorScheme.error.copy(alpha = 0.14f) to
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
                    text      = fb.message,
                    color     = fg,
                    style     = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center
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
                style         = MaterialTheme.typography.labelSmall,
                color         = MaterialTheme.colorScheme.tertiary,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text       = hint,
                style      = MaterialTheme.typography.bodyMedium,
                color      = MaterialTheme.colorScheme.onSurface,
                textAlign  = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}

// ── Input row ─────────────────────────────────────────────────────────────────

@Composable
private fun InputRow(
    text          : String,
    enabled       : Boolean,
    validating    : Boolean,
    focusRequester: FocusRequester,
    onChange      : (String) -> Unit,
    onSubmit      : () -> Unit
) {
    Column(
        modifier            = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value         = text,
            onValueChange = onChange,
            enabled       = enabled,
            placeholder   = {
                Text(
                    text  = "Type the next word…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            },
            singleLine      = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            trailingIcon    = {
                if (validating) CircularProgressIndicator(
                    modifier    = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color       = MaterialTheme.colorScheme.primary
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f),
                focusedTextColor     = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor   = MaterialTheme.colorScheme.onSurface,
                cursorColor          = MaterialTheme.colorScheme.primary
            ),
            shape    = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )
        Button(
            onClick  = onSubmit,
            enabled  = enabled && text.isNotBlank() && !validating,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(text = "Submit", style = MaterialTheme.typography.labelLarge)
        }
    }
}

// ── Control section ───────────────────────────────────────────────────────────

@Composable
private fun ControlSection(
    phase    : GamePhase,
    score    : Int,
    bestScore: Int,
    onStart  : () -> Unit,
    onRestart: () -> Unit
) {
    AnimatedContent(
        targetState  = phase,
        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
        label        = "control"
    ) { p ->
        when (p) {
            GamePhase.GAME_OVER -> GameOverCard(score, bestScore, onRestart)
            else                -> StartButton(loading = p == GamePhase.LOADING, onClick = onStart)
        }
    }
}

@Composable
private fun StartButton(loading: Boolean, onClick: () -> Unit) {
    Button(
        onClick  = onClick,
        enabled  = !loading,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape    = RoundedCornerShape(14.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor   = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier    = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color       = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(text = "Start Game", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun GameOverCard(score: Int, bestScore: Int, onRestart: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(20.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text       = "Game Over",
                style      = MaterialTheme.typography.headlineMedium,
                color      = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Bold
            )
            Text(
                text  = "Score: $score",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            if (bestScore > 0) {
                Text(
                    text  = "Best: $bestScore",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onRestart,
                shape   = RoundedCornerShape(12.dp),
                colors  = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor   = Color.White
                )
            ) {
                Text(text = "Play Again", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}