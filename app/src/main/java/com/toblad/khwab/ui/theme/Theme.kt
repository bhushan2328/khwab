package com.toblad.khwab.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val KhwabColorScheme = darkColorScheme(
    primary = KhwabBlue,
    secondary = KhwabGreen,
    tertiary = KhwabRed,

    background = KhwabBackground,
    surface = KhwabSurface,
    surfaceVariant = KhwabCard,

    onPrimary = KhwabWhite,
    onSecondary = KhwabWhite,
    onTertiary = KhwabWhite,

    onBackground = KhwabWhite,
    onSurface = KhwabWhite,
    onSurfaceVariant = KhwabWhite
)

@Composable
fun KhwabTheme(
    content: @Composable () -> Unit
) {
    val colors = when (ThemeController.currentTheme) {
        ThemeMode.AURA -> ThemeController.currentAuraColors
        ThemeMode.DEFAULT -> KhwabColorScheme
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}