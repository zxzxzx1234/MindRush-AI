package com.example.mindrushai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Colour tokens ─────────────────────────────────────────────────────────────

object MindRushColors {
    val Primary          = Color(0xFF7B8EF7)
    val OnPrimary        = Color(0xFF0D0D1A)
    val Secondary        = Color(0xFF5CC8A0)
    val OnSecondary      = Color(0xFF0D1A14)
    val Tertiary         = Color(0xFFF7A25B)
    val OnTertiary       = Color(0xFF1A100A)
    val Background       = Color(0xFF0D0D1A)
    val Surface          = Color(0xFF161625)
    val SurfaceVariant   = Color(0xFF1E1E30)
    val OnBackground     = Color(0xFFE8E8F0)
    val OnSurface        = Color(0xFFE8E8F0)
    val OnSurfaceVariant = Color(0xFFB0B0C8)
    val Error            = Color(0xFFE07070)
    val ErrorContainer   = Color(0xFF2A1515)
    val OnErrorContainer = Color(0xFFE0A0A0)
}

private val MindRushColorScheme = darkColorScheme(
    primary          = MindRushColors.Primary,
    onPrimary        = MindRushColors.OnPrimary,
    secondary        = MindRushColors.Secondary,
    onSecondary      = MindRushColors.OnSecondary,
    tertiary         = MindRushColors.Tertiary,
    onTertiary       = MindRushColors.OnTertiary,
    background       = MindRushColors.Background,
    surface          = MindRushColors.Surface,
    surfaceVariant   = MindRushColors.SurfaceVariant,
    onBackground     = MindRushColors.OnBackground,
    onSurface        = MindRushColors.OnSurface,
    onSurfaceVariant = MindRushColors.OnSurfaceVariant,
    error            = MindRushColors.Error,
    errorContainer   = MindRushColors.ErrorContainer,
    onErrorContainer = MindRushColors.OnErrorContainer
)

private val MindRushTypography = Typography(
    displayLarge   = TextStyle(fontWeight = FontWeight.Bold,     fontSize = 32.sp, letterSpacing = (-0.5).sp),
    headlineLarge  = TextStyle(fontWeight = FontWeight.Bold,     fontSize = 26.sp, letterSpacing = 0.5.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleLarge     = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium    = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 15.sp),
    bodyLarge      = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium     = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge     = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.1.sp),
    labelMedium    = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 12.sp, letterSpacing = 0.5.sp),
    labelSmall     = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 10.sp, letterSpacing = 1.5.sp)
)

@Composable
fun MindRushTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MindRushColorScheme,
        typography  = MindRushTypography,
        content     = content
    )
}