package com.toblad.khwab.ui.theme

import androidx.compose.material3.darkColorScheme

/**
 * Color scheme used when Aura is active.
 *
 * Currently this mirrors the default Khwab theme.
 *
 * Later these colors will be generated dynamically
 * by AuraEngine based on:
 *
 * • Time of day
 * • Weather
 * • Environment
 * • Lighting
 * • User preferences
 */
val AuraColorScheme = darkColorScheme(

    // Primary Colors
    primary = KhwabBlue,
    secondary = KhwabGreen,
    tertiary = KhwabRed,

    // Backgrounds
    background = KhwabBackground,
    surface = KhwabSurface,
    surfaceVariant = KhwabCard,

    // Text
    onPrimary = KhwabWhite,
    onSecondary = KhwabWhite,
    onTertiary = KhwabWhite,

    onBackground = KhwabWhite,
    onSurface = KhwabWhite,
    onSurfaceVariant = KhwabWhite
)