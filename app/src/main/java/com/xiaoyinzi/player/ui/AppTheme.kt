package com.xiaoyinzi.player.ui

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.xiaoyinzi.player.data.AppThemeDefinition
import com.xiaoyinzi.player.data.BuiltInThemes

internal val LocalAppTheme = staticCompositionLocalOf { BuiltInThemes.default }
internal val LocalAccentText = staticCompositionLocalOf { Color(0xFF856126) }

internal fun readableText(background: Color): Color {
    val dark = Color(0xFF20373A)
    val light = Color(0xFFECF2F1)
    val preferred =
        if (contrastRatio(background, dark) >= contrastRatio(background, light)) dark else light
    if (contrastRatio(background, preferred) >= 4.5f) return preferred
    return if (contrastRatio(background, Color.Black) >= contrastRatio(background, Color.White))
        Color.Black
    else Color.White
}

internal fun secondaryText(background: Color): Color =
    readableAccent(lerp(readableText(background), background, .28f), background)

private fun contrastRatio(first: Color, second: Color): Float {
    val a = first.luminance()
    val b = second.luminance()
    return (maxOf(a, b) + .05f) / (minOf(a, b) + .05f)
}

internal fun readableAccent(accent: Color, background: Color): Color {
    var adjusted = accent
    val ink = readableText(background)
    repeat(16) {
        if (contrastRatio(adjusted, background) >= 4.5f) return adjusted
        adjusted = lerp(adjusted, ink, .18f)
    }
    return ink
}

@Composable
fun XiaoYinZiTheme(
    theme: AppThemeDefinition = BuiltInThemes.default,
    content: @Composable () -> Unit,
) {
    val scheme =
        remember(theme.accent, theme.background, theme.panel) {
            val background = Color(theme.background)
            val panel = Color(theme.panel)
            val accent = Color(theme.accent)
            val accessibleAccent = readableAccent(accent, background)
            val ink = readableText(background)
            val panelInk = readableText(panel)
            val primaryContainer = lerp(background, accent, .18f)
            val base = if (background.luminance() < .35f) darkColorScheme() else lightColorScheme()
            base.copy(
                primary = accessibleAccent,
                onPrimary = readableText(accessibleAccent),
                primaryContainer = primaryContainer,
                onPrimaryContainer = readableText(primaryContainer),
                background = background,
                onBackground = ink,
                surface = background,
                onSurface = ink,
                surfaceVariant = panel,
                onSurfaceVariant = secondaryText(background),
                surfaceContainerLowest = background,
                surfaceContainerLow = lerp(background, panel, .35f),
                surfaceContainer = lerp(background, panel, .55f),
                surfaceContainerHigh = lerp(background, panel, .75f),
                surfaceContainerHighest = panel,
                outline = lerp(ink, background, .65f),
                outlineVariant = lerp(ink, background, .85f),
                secondary = readableAccent(lerp(accent, ink, .25f), background),
                onSecondary = background,
                secondaryContainer = panel,
                onSecondaryContainer = panelInk,
                surfaceTint = accent,
                inverseSurface = ink,
                inverseOnSurface = background,
            )
        }
    CompositionLocalProvider(
        LocalAppTheme provides theme,
        LocalAccentText provides readableAccent(Color(theme.accent), Color(theme.background)),
        LocalContentColor provides scheme.onBackground,
    ) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
    }
}
