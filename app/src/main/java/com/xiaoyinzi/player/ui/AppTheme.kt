package com.xiaoyinzi.player.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Ink = Color(0xFF20373A)
private val Paper = Color(0xFFF5F1E7)
private val Muted = Color(0xFF718083)
private val Firefly = Color(0xFFB98A43)
private val Celadon = Color(0xFFDDE7DF)

private val colors = lightColorScheme(
    primary = Firefly,
    onPrimary = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Celadon,
    onSurfaceVariant = Muted,
    outline = Color(0xFFB9C5C0),
    outlineVariant = Color(0xFFD7DDD8),
    secondary = Color(0xFF607F83),
    onSecondary = Color.White,
    surfaceTint = Firefly,
)

@Composable
fun XiaoYinZiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
}
