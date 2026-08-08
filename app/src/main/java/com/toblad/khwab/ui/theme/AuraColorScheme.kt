package com.toblad.khwab.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.toblad.khwab.aura.model.AmbientLightStyle
import com.toblad.khwab.aura.model.SkyStyle
import com.toblad.khwab.aura.model.ThemeProfile
import com.toblad.khwab.aura.model.WeatherEffectStyle
import kotlin.math.pow

/**
 * Builds a Material3 [ColorScheme] from Aura's current
 * [ThemeProfile] — driven by real sky, ambient light, and
 * weather conditions rather than a single fixed palette.
 */
/**
 * Returns white or a near-black depending on which gives the
 * higher WCAG contrast ratio against [background].
 * Uses the sRGB relative-luminance formula so it adapts to every
 * Aura sky/accent colour automatically.
 */
private fun contrastOn(background: Color): Color {
    fun linearise(c: Float) =
        if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)

    val r = linearise(background.red)
    val g = linearise(background.green)
    val b = linearise(background.blue)
    val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b

    // WCAG: contrast against white = (1 + 0.05) / (L + 0.05)
    //       contrast against dark  = (L + 0.05) / (0.05 + 0.05) … simplified
    return if (luminance > 0.179f) Color(0xFF13101F) else Color(0xFFF5F3FF)
}

fun auraColorScheme(profile: ThemeProfile): ColorScheme {

    val sky = skyPalette(profile.sky)
    val accent = ambientAccent(profile.ambientLight)

    val background = weatherAdjust(sky.background, profile.weatherEffect)
    val surface = weatherAdjust(sky.surface, profile.weatherEffect)

    return darkColorScheme(
        primary = accent,
        onPrimary = contrastOn(accent),

        secondary = sky.secondary,
        onSecondary = contrastOn(sky.secondary),

        tertiary = sky.tertiary,
        onTertiary = contrastOn(sky.tertiary),

        background = background,
        onBackground = sky.onSurface,

        surface = surface,
        onSurface = sky.onSurface,

        // onSurfaceVariant is intentionally dimmer than onSurface — used for
        // secondary labels, captions, and hints; gives real text hierarchy.
        surfaceVariant = sky.surfaceVariant,
        onSurfaceVariant = sky.onSurface.copy(alpha = 0.65f),

        // Outline uses surfaceVariant so dividers are subtle ink, not a
        // washed-out ghost of the text colour.
        outline = sky.surfaceVariant
    )
}

private data class SkyPalette(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val secondary: Color,
    val tertiary: Color,
    val onSurface: Color
)

private fun skyPalette(sky: SkyStyle): SkyPalette = when (sky) {

    // PRE_DAWN: deep indigo with a hint of warmth — darker than MIDNIGHT's
    // cold blue-black so the two phases read as visually distinct.
    SkyStyle.PRE_DAWN -> SkyPalette(
        background = Color(0xFF0E1028),
        surface = Color(0xFF171938),
        surfaceVariant = Color(0xFF232650),
        secondary = Color(0xFF7A7FD6),
        tertiary = Color(0xFFBE97E8),
        onSurface = Color(0xFFE8E5FF)
    )

    SkyStyle.DAWN,
    SkyStyle.SUNRISE -> SkyPalette(
        background = Color(0xFF241831),
        surface = Color(0xFF352142),
        surfaceVariant = Color(0xFF4A2C4E),
        secondary = Color(0xFFFF9A6C),
        tertiary = Color(0xFFFFC98C),
        onSurface = Color(0xFFFFEFE6)
    )

    SkyStyle.MORNING -> SkyPalette(
        background = Color(0xFF10203A),
        surface = Color(0xFF16324F),
        surfaceVariant = Color(0xFF1F4666),
        secondary = Color(0xFF5FC7FF),
        tertiary = Color(0xFFFFD98C),
        onSurface = Color(0xFFEAF6FF)
    )

    SkyStyle.NOON -> SkyPalette(
        background = Color(0xFF0B2A4A),
        surface = Color(0xFF0F3A63),
        surfaceVariant = Color(0xFF17527F),
        secondary = Color(0xFF3AD1FF),
        tertiary = Color(0xFFFFE28C),
        onSurface = Color(0xFFF2FAFF)
    )

    SkyStyle.AFTERNOON -> SkyPalette(
        background = Color(0xFF1B2B45),
        surface = Color(0xFF243A5C),
        surfaceVariant = Color(0xFF32507A),
        secondary = Color(0xFF6FB6E0),
        tertiary = Color(0xFFFFC978),
        onSurface = Color(0xFFF0F4FA)
    )

    SkyStyle.SUNSET -> SkyPalette(
        background = Color(0xFF321328),
        surface = Color(0xFF451A38),
        surfaceVariant = Color(0xFF5E2149),
        secondary = Color(0xFFFF7A5C),
        tertiary = Color(0xFFFFB870),
        onSurface = Color(0xFFFFECE4)
    )

    SkyStyle.EVENING -> SkyPalette(
        background = Color(0xFF1B1533),
        surface = Color(0xFF261E45),
        surfaceVariant = Color(0xFF352A5E),
        secondary = Color(0xFF8E7CE0),
        tertiary = Color(0xFFE08CC7),
        onSurface = Color(0xFFECE6FF)
    )

    SkyStyle.NIGHT -> SkyPalette(
        background = Color(0xFF0B0E24),
        surface = Color(0xFF12163A),
        surfaceVariant = Color(0xFF1B2050),
        secondary = Color(0xFF6E7BD6),
        tertiary = Color(0xFFB39DFF),
        onSurface = Color(0xFFE0E3FF)
    )

    // MIDNIGHT: coldest, deepest — near-black with icy blue undertone.
    // Distinct from PRE_DAWN by being cooler and having less warm violet.
    SkyStyle.MIDNIGHT -> SkyPalette(
        background = Color(0xFF04050F),
        surface = Color(0xFF080A1E),
        surfaceVariant = Color(0xFF0F1230),
        secondary = Color(0xFF4A55B0),
        tertiary = Color(0xFF8470CC),
        onSurface = Color(0xFFBFC3EE)
    )

    // CLOUDY: warm-grey cast — feels like an overcast sky, not cold concrete.
    SkyStyle.CLOUDY -> SkyPalette(
        background = Color(0xFF1F2229),
        surface = Color(0xFF2B2F38),
        surfaceVariant = Color(0xFF3B4049),
        secondary = Color(0xFF8E9BAD),
        tertiary = Color(0xFFB8C1CC),
        onSurface = Color(0xFFE8EAF0)
    )

    // FOG: slightly warmer and more desaturated than CLOUDY — milky haze feel.
    SkyStyle.FOG -> SkyPalette(
        background = Color(0xFF252729),
        surface = Color(0xFF333537),
        surfaceVariant = Color(0xFF424547),
        secondary = Color(0xFF9BA4A8),
        tertiary = Color(0xFFBFC6C9),
        onSurface = Color(0xFFE8EAEB)
    )

    // STORM: dark and dramatic but not crushed to pure black.
    // Reduced weather darken (0.16→0.10) keeps surface colours readable.
    SkyStyle.STORM -> SkyPalette(
        background = Color(0xFF131620),
        surface = Color(0xFF1A1E2A),
        surfaceVariant = Color(0xFF242933),
        secondary = Color(0xFF607590),
        tertiary = Color(0xFFB0BCC8),
        onSurface = Color(0xFFD8DDE5)
    )
}

private fun ambientAccent(light: AmbientLightStyle): Color = when (light) {
    // PRE_DAWN: brighter periwinkle — visible against the very dark background
    AmbientLightStyle.PRE_DAWN -> Color(0xFF9BA4F0)
    AmbientLightStyle.SUNRISE -> Color(0xFFFFA36C)
    AmbientLightStyle.MORNING -> Color(0xFF6FCBFF)
    AmbientLightStyle.NOON -> Color(0xFF3AD1FF)
    AmbientLightStyle.AFTERNOON -> Color(0xFFFFC978)
    AmbientLightStyle.SUNSET -> Color(0xFFFF8A65)
    // EVENING: richer violet for contrast against dark purple background
    AmbientLightStyle.EVENING -> Color(0xFFC4B0FF)
    // MOONLIGHT: slightly more luminous so it reads on NIGHT backgrounds
    AmbientLightStyle.MOONLIGHT -> Color(0xFFADB7FF)
    // NIGHT: brighter blue so it doesn't sink into the dark navy background
    AmbientLightStyle.NIGHT -> Color(0xFF7D8EE8)
    AmbientLightStyle.OVERCAST -> Color(0xFFAAB2BC)
    AmbientLightStyle.FOG -> Color(0xFFBFC6C9)
}

private fun weatherAdjust(color: Color, weather: WeatherEffectStyle): Color {

    val darken = when (weather) {
        WeatherEffectStyle.NONE -> 0f
        WeatherEffectStyle.FOG -> 0.05f
        WeatherEffectStyle.RAIN -> 0.08f
        WeatherEffectStyle.SNOW -> 0.03f
        // Reduced from 0.16 → 0.10: STORM sky bases are already dark; over-
        // darkening crushed them to near-black and lost surface distinction.
        WeatherEffectStyle.STORM -> 0.10f
    }

    if (darken == 0f) return color

    return Color(
        red = color.red * (1f - darken),
        green = color.green * (1f - darken),
        blue = color.blue * (1f - darken),
        alpha = color.alpha
    )
}