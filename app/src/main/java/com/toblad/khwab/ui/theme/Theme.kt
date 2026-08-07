package com.toblad.khwab.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KhwabDarkColorScheme = darkColorScheme(
    primary             = KhwabBlue,
    primaryContainer    = KhwabBlueContainer,
    secondary           = KhwabGreen,
    secondaryContainer  = KhwabGreenContainer,
    tertiary            = KhwabViolet,
    tertiaryContainer   = KhwabVioletContainer,
    error               = KhwabRed,
    errorContainer      = KhwabRedContainer,

    background          = KhwabBackground,
    surface             = KhwabSurface,
    surfaceVariant      = KhwabCard,
    outline             = KhwabBorder,

    onPrimary           = KhwabWhite,
    onSecondary         = KhwabWhite,
    onTertiary          = KhwabWhite,
    onError             = KhwabWhite,
    onBackground        = KhwabTextPrimary,
    onSurface           = KhwabTextPrimary,
    onSurfaceVariant    = KhwabTextSecondary
)

private val KhwabLightColorScheme = lightColorScheme(
    primary             = KhwabBlueDark,
    primaryContainer    = Color(0xFFD6E8FF),
    secondary           = Color(0xFF00875A),
    tertiary            = Color(0xFF6A3FC7),
    error               = KhwabRed,

    background          = KhwabBackgroundLight,
    surface             = KhwabSurfaceLight,
    surfaceVariant      = KhwabCardLight,
    outline             = KhwabBorderLight,

    onPrimary           = KhwabWhite,
    onSecondary         = KhwabWhite,
    onTertiary          = KhwabWhite,
    onBackground        = Color(0xFF0D1120),
    onSurface           = Color(0xFF0D1120),
    onSurfaceVariant    = Color(0xFF3A4460)
)

@Composable
fun KhwabTheme(
    content: @Composable () -> Unit
) {
    val colors = when (ThemeController.currentTheme) {
        ThemeMode.AURA    -> ThemeController.currentAuraColors
        ThemeMode.DEFAULT -> if (isSystemInDarkTheme()) KhwabDarkColorScheme
                             else KhwabLightColorScheme
    }

    MaterialTheme(
        colorScheme = colors,
        typography  = Typography,
        content     = content
    )
}
