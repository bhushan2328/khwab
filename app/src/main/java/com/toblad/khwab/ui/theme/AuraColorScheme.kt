package com.toblad.khwab.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.toblad.khwab.aura.model.AmbientLightStyle
import com.toblad.khwab.aura.model.SkyStyle
import com.toblad.khwab.aura.model.ThemeProfile
import com.toblad.khwab.aura.model.WeatherEffectStyle

/**
 * Builds a Material3 [ColorScheme] from Aura's current
 * [ThemeProfile] — driven by real sky, ambient light, and
 * weather conditions rather than a single fixed palette.
 */
fun auraColorScheme(profile: ThemeProfile): ColorScheme {

    val sky = skyPalette(profile.sky)
    val accent = ambientAccent(profile.ambientLight)

    val background = weatherAdjust(sky.background, profile.weatherEffect)
    val surface = weatherAdjust(sky.surface, profile.weatherEffect)

    return darkColorScheme(
        primary = accent,
        onPrimary = Color(0xFF13101F),

        secondary = sky.secondary,
        onSecondary = Color(0xFF13101F),

        tertiary = sky.tertiary,
        onTertiary = Color(0xFF13101F),

        background = background,
        onBackground = sky.onSurface,

        surface = surface,
        onSurface = sky.onSurface,

        surfaceVariant = sky.surfaceVariant,
        onSurfaceVariant = sky.onSurface,

        outline = sky.onSurface.copy(alpha = 0.5f)
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

    SkyStyle.PRE_DAWN -> SkyPalette(
        background = Color(0xFF10122B),
        surface = Color(0xFF191B3D),
        surfaceVariant = Color(0xFF262A52),
        secondary = Color(0xFF6F73C9),
        tertiary = Color(0xFFB18CD9),
        onSurface = Color(0xFFE4E1FA)
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

    SkyStyle.MIDNIGHT -> SkyPalette(
        background = Color(0xFF05061A),
        surface = Color(0xFF0A0C2A),
        surfaceVariant = Color(0xFF12153D),
        secondary = Color(0xFF4F5AB8),
        tertiary = Color(0xFF8C7BD9),
        onSurface = Color(0xFFC9CCF2)
    )

    SkyStyle.CLOUDY -> SkyPalette(
        background = Color(0xFF23262E),
        surface = Color(0xFF30343E),
        surfaceVariant = Color(0xFF40454F),
        secondary = Color(0xFF9AA4B2),
        tertiary = Color(0xFFC7CCD4),
        onSurface = Color(0xFFEDEFF2)
    )

    SkyStyle.FOG -> SkyPalette(
        background = Color(0xFF2B2E31),
        surface = Color(0xFF383B3F),
        surfaceVariant = Color(0xFF484C50),
        secondary = Color(0xFFA9B0B4),
        tertiary = Color(0xFFCBD0D2),
        onSurface = Color(0xFFEDEFEF)
    )

    SkyStyle.STORM -> SkyPalette(
        background = Color(0xFF15181D),
        surface = Color(0xFF1E2228),
        surfaceVariant = Color(0xFF2A2F36),
        secondary = Color(0xFF6E86A6),
        tertiary = Color(0xFFC7D0DC),
        onSurface = Color(0xFFE3E7EC)
    )
}

private fun ambientAccent(light: AmbientLightStyle): Color = when (light) {
    AmbientLightStyle.PRE_DAWN -> Color(0xFF8890E8)
    AmbientLightStyle.SUNRISE -> Color(0xFFFFA36C)
    AmbientLightStyle.MORNING -> Color(0xFF6FCBFF)
    AmbientLightStyle.NOON -> Color(0xFF3AD1FF)
    AmbientLightStyle.AFTERNOON -> Color(0xFFFFC978)
    AmbientLightStyle.SUNSET -> Color(0xFFFF8A65)
    AmbientLightStyle.EVENING -> Color(0xFFB39DFF)
    AmbientLightStyle.MOONLIGHT -> Color(0xFF9FA8FF)
    AmbientLightStyle.NIGHT -> Color(0xFF6E7BD6)
    AmbientLightStyle.OVERCAST -> Color(0xFFAAB2BC)
    AmbientLightStyle.FOG -> Color(0xFFBFC6C9)
}

private fun weatherAdjust(color: Color, weather: WeatherEffectStyle): Color {

    val darken = when (weather) {
        WeatherEffectStyle.NONE -> 0f
        WeatherEffectStyle.FOG -> 0.05f
        WeatherEffectStyle.RAIN -> 0.08f
        WeatherEffectStyle.SNOW -> 0.03f
        WeatherEffectStyle.STORM -> 0.16f
    }

    if (darken == 0f) return color

    return Color(
        red = color.red * (1f - darken),
        green = color.green * (1f - darken),
        blue = color.blue * (1f - darken),
        alpha = color.alpha
    )
}