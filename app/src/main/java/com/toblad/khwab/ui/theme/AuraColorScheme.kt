package com.toblad.khwab.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.toblad.khwab.aura.model.TimePhase
import com.toblad.khwab.aura.model.WeatherState
import kotlin.math.pow

/**
 * Builds a Material3 [ColorScheme] from Aura's current [WeatherState] and [TimePhase].
 *
 * This replaces the old ThemeProfile-based scheme that depended on the 2D :aura module.
 * Colors now map directly from the two primary dimensions (time + weather) that the
 * Android UI layer observes — Unity handles the full visual rendering independently.
 */

/**
 * Returns white or near-black depending on which gives the higher WCAG contrast ratio
 * against [background].
 */
private fun contrastOn(background: Color): Color {
    fun linearise(c: Float) =
        if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)

    val r = linearise(background.red)
    val g = linearise(background.green)
    val b = linearise(background.blue)
    val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b

    return if (luminance > 0.179f) Color(0xFF13101F) else Color(0xFFF5F3FF)
}

/**
 * Derives a full Material3 [ColorScheme] from [weather] + [timePhase].
 *
 * Time phase drives background / surface / accent tones (sky palette).
 * Weather further adjusts background darkness (overcast/storm = darker).
 */
fun auraColorScheme(weather: WeatherState, timePhase: TimePhase): ColorScheme {
    val sky = skyPalette(timePhase, weather)
    val accent = accentFor(timePhase, weather)

    return darkColorScheme(
        primary = accent,
        onPrimary = contrastOn(accent),

        secondary = sky.secondary,
        onSecondary = contrastOn(sky.secondary),

        tertiary = sky.tertiary,
        onTertiary = contrastOn(sky.tertiary),

        background = sky.background,
        onBackground = sky.onSurface,

        surface = sky.surface,
        onSurface = sky.onSurface,

        surfaceVariant = sky.surfaceVariant,
        onSurfaceVariant = sky.onSurface.copy(alpha = 0.65f),

        outline = sky.surfaceVariant
    )
}

// ── Private data ──────────────────────────────────────────────────────────────

private data class SkyPalette(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val secondary: Color,
    val tertiary: Color,
    val onSurface: Color
)

/**
 * Maps [TimePhase] + [WeatherState] to a sky palette.
 * Weather darkens the background for overcast/storm conditions.
 */
private fun skyPalette(phase: TimePhase, weather: WeatherState): SkyPalette {
    val base = basePalette(phase)
    return when (weather) {
        WeatherState.STORM   -> base.darken(0.10f)
        WeatherState.RAIN    -> base.darken(0.08f)
        WeatherState.FOG     -> base.darken(0.05f)
        WeatherState.CLOUDY  -> base.darken(0.04f)
        WeatherState.SNOW    -> base.darken(0.03f)
        WeatherState.CLEAR   -> base
    }
}

/** Applies a uniform darkness fraction to background + surface channels. */
private fun SkyPalette.darken(fraction: Float): SkyPalette = copy(
    background = background.scale(1f - fraction),
    surface    = surface.scale(1f - fraction)
)

private fun Color.scale(f: Float) = Color(
    red   = red   * f,
    green = green * f,
    blue  = blue  * f,
    alpha = alpha
)

/** Accent color that shifts across the day. Weather darkens it for mood. */
private fun accentFor(phase: TimePhase, weather: WeatherState): Color {
    val base = when (phase) {
        TimePhase.PRE_DAWN  -> Color(0xFF9BA4F0)
        TimePhase.SUNRISE   -> Color(0xFFFFA36C)
        TimePhase.MORNING   -> Color(0xFF6FCBFF)
        TimePhase.NOON      -> Color(0xFF3AD1FF)
        TimePhase.AFTERNOON -> Color(0xFFFFC978)
        TimePhase.SUNSET    -> Color(0xFFFF8A65)
        TimePhase.EVENING   -> Color(0xFFC4B0FF)
        TimePhase.NIGHT     -> Color(0xFF7D8EE8)
        TimePhase.MIDNIGHT  -> Color(0xFF4A55B0)
    }
    return when (weather) {
        WeatherState.STORM  -> base.scale(0.75f)
        WeatherState.RAIN   -> base.scale(0.85f)
        WeatherState.FOG    -> base.scale(0.90f)
        else                -> base
    }
}

/** Base sky palette driven by time phase alone. */
private fun basePalette(phase: TimePhase): SkyPalette = when (phase) {
    TimePhase.PRE_DAWN  -> SkyPalette(
        background    = Color(0xFF0E1028),
        surface       = Color(0xFF171938),
        surfaceVariant= Color(0xFF232650),
        secondary     = Color(0xFF7A7FD6),
        tertiary      = Color(0xFFBE97E8),
        onSurface     = Color(0xFFE8E5FF)
    )
    TimePhase.SUNRISE   -> SkyPalette(
        background    = Color(0xFF241831),
        surface       = Color(0xFF352142),
        surfaceVariant= Color(0xFF4A2C4E),
        secondary     = Color(0xFFFF9A6C),
        tertiary      = Color(0xFFFFC98C),
        onSurface     = Color(0xFFFFEFE6)
    )
    TimePhase.MORNING   -> SkyPalette(
        background    = Color(0xFF10203A),
        surface       = Color(0xFF16324F),
        surfaceVariant= Color(0xFF1F4666),
        secondary     = Color(0xFF5FC7FF),
        tertiary      = Color(0xFFFFD98C),
        onSurface     = Color(0xFFEAF6FF)
    )
    TimePhase.NOON      -> SkyPalette(
        background    = Color(0xFF0B2A4A),
        surface       = Color(0xFF0F3A63),
        surfaceVariant= Color(0xFF17527F),
        secondary     = Color(0xFF3AD1FF),
        tertiary      = Color(0xFFFFE28C),
        onSurface     = Color(0xFFF2FAFF)
    )
    TimePhase.AFTERNOON -> SkyPalette(
        background    = Color(0xFF1B2B45),
        surface       = Color(0xFF243A5C),
        surfaceVariant= Color(0xFF32507A),
        secondary     = Color(0xFF6FB6E0),
        tertiary      = Color(0xFFFFC978),
        onSurface     = Color(0xFFF0F4FA)
    )
    TimePhase.SUNSET    -> SkyPalette(
        background    = Color(0xFF321328),
        surface       = Color(0xFF451A38),
        surfaceVariant= Color(0xFF5E2149),
        secondary     = Color(0xFFFF7A5C),
        tertiary      = Color(0xFFFFB870),
        onSurface     = Color(0xFFFFECE4)
    )
    TimePhase.EVENING   -> SkyPalette(
        background    = Color(0xFF1B1533),
        surface       = Color(0xFF261E45),
        surfaceVariant= Color(0xFF352A5E),
        secondary     = Color(0xFF8E7CE0),
        tertiary      = Color(0xFFE08CC7),
        onSurface     = Color(0xFFECE6FF)
    )
    TimePhase.NIGHT     -> SkyPalette(
        background    = Color(0xFF0B0E24),
        surface       = Color(0xFF12163A),
        surfaceVariant= Color(0xFF1B2050),
        secondary     = Color(0xFF6E7BD6),
        tertiary      = Color(0xFFB39DFF),
        onSurface     = Color(0xFFE0E3FF)
    )
    TimePhase.MIDNIGHT  -> SkyPalette(
        background    = Color(0xFF04050F),
        surface       = Color(0xFF080A1E),
        surfaceVariant= Color(0xFF0F1230),
        secondary     = Color(0xFF4A55B0),
        tertiary      = Color(0xFF8470CC),
        onSurface     = Color(0xFFBFC3EE)
    )
}
