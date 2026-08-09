package com.toblad.khwab.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Khwab base theme: pure black background, electric blue primary,
 * teal-green secondary. No light mode — the app is always dark.
 *
 * When Aura is active [ThemeMode.AURA] the color scheme is replaced
 * entirely by [auraColorScheme] driven by real sky / weather / time.
 */
private val KhwabColorScheme = darkColorScheme(
    // ── Accents ──────────────────────────────────────────────────────────────
    primary             = KhwabBlue,
    primaryContainer    = KhwabBlueContainer,
    onPrimary           = KhwabWhite,
    onPrimaryContainer  = KhwabBlueLight,

    secondary           = KhwabGreen,
    secondaryContainer  = KhwabGreenContainer,
    onSecondary         = KhwabBackground,      // black text on vivid green
    onSecondaryContainer= KhwabGreen,

    tertiary            = KhwabViolet,
    tertiaryContainer   = KhwabVioletContainer,
    onTertiary          = KhwabWhite,
    onTertiaryContainer = KhwabViolet,

    // ── Danger ───────────────────────────────────────────────────────────────
    error               = KhwabRed,
    errorContainer      = KhwabRedContainer,
    onError             = KhwabWhite,
    onErrorContainer    = KhwabRed,

    // ── Backgrounds & surfaces ────────────────────────────────────────────────
    background          = KhwabBackground,      // 0xFF000000 — pure OLED black
    onBackground        = KhwabTextPrimary,     // light blue-white text

    surface             = KhwabSurface,
    onSurface           = KhwabTextPrimary,

    surfaceVariant      = KhwabCard,
    onSurfaceVariant    = KhwabTextSecondary,   // sky-blue secondary text

    outline             = KhwabBorder,
    outlineVariant      = KhwabBorder,

    // ── Inverse (used by Snackbar, tooltips) ─────────────────────────────────
    inverseSurface      = KhwabTextPrimary,
    inverseOnSurface    = KhwabBackground,
    inversePrimary      = KhwabBlue,
)

@Composable
fun KhwabTheme(
    content: @Composable () -> Unit
) {
    val colors = when (ThemeController.currentTheme) {
        ThemeMode.AURA    -> ThemeController.currentAuraColors
        ThemeMode.DEFAULT -> KhwabColorScheme
    }

    MaterialTheme(
        colorScheme = colors,
        typography  = Typography,
        content     = content
    )
}
