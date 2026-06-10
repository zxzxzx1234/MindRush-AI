package com.example.mindrushai.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mindrushai.data.GameStats

/**
 * StatsScreen
 *
 * Displays session and lifetime statistics with animated accuracy rings.
 */
@Composable
fun StatsScreen(
    stats    : GameStats,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text       = "Statistics",
                style      = MaterialTheme.typography.headlineLarge,
                color      = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onDismiss) {
                Text(
                    text  = "← Back",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Column(
            modifier            = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Session section ───────────────────────────────────────────────
            SectionHeader("This Session")

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AccuracyRing(
                    modifier = Modifier.weight(1f),
                    accuracy = stats.sessionAccuracy,
                    label    = "Accuracy",
                    color    = MaterialTheme.colorScheme.primary
                )
                Column(
                    modifier            = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard("Score",  "${stats.sessionScore}")
                    StatCard("Rounds", "${stats.sessionRounds}")
                }
            }

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label    = "Best Streak",
                    value    = "🔥 ${stats.sessionBestStreak}",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label    = "Avg Speed",
                    value    = if (stats.sessionAvgResponseMs > 0L)
                        "${"%.1f".format(stats.sessionAvgResponseMs / 1000.0)}s"
                    else "—",
                    modifier = Modifier.weight(1f)
                )
            }

            StatCard(
                label    = "Words Correct",
                value    = "${stats.sessionWordsCorrect} / ${stats.sessionWordsAttempted}",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(4.dp))

            // ── Lifetime section ──────────────────────────────────────────────
            SectionHeader("All Time")

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AccuracyRing(
                    modifier = Modifier.weight(1f),
                    accuracy = stats.lifetimeAccuracy,
                    label    = "Accuracy",
                    color    = MaterialTheme.colorScheme.secondary
                )
                Column(
                    modifier            = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard("Best Score", "${stats.lifetimeBestScore}")
                    StatCard("Games",      "${stats.lifetimeTotalGames}")
                }
            }

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label    = "Total Rounds",
                    value    = "${stats.lifetimeTotalRounds}",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label    = "Words Recalled",
                    value    = "${stats.lifetimeWordsCorrect}",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text          = title.uppercase(),
        style         = MaterialTheme.typography.labelSmall,
        color         = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 2.sp
    )
}

// ── Stat card ─────────────────────────────────────────────────────────────────

@Composable
private fun StatCard(
    label   : String,
    value   : String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text       = value,
            style      = MaterialTheme.typography.titleLarge,
            color      = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text      = label,
            style     = MaterialTheme.typography.labelSmall,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ── Accuracy ring ─────────────────────────────────────────────────────────────

@Composable
private fun AccuracyRing(
    accuracy : Float,
    label    : String,
    color    : Color,
    modifier : Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue   = accuracy.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label         = "accuracyRing"
    )

    val trackColor = color.copy(alpha = 0.15f)
    val percentage = (accuracy * 100).toInt()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .drawBehind {
                    val strokeWidth = 10.dp.toPx()
                    val inset       = strokeWidth / 2
                    val arcSize     = Size(
                        size.width  - strokeWidth,
                        size.height - strokeWidth
                    )
                    // Track arc
                    drawArc(
                        color      = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter  = false,
                        topLeft    = Offset(inset, inset),
                        size       = arcSize,
                        style      = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                    // Progress arc
                    drawArc(
                        color      = color,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter  = false,
                        topLeft    = Offset(inset, inset),
                        size       = arcSize,
                        style      = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text       = "$percentage%",
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text  = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}