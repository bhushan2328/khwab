package com.toblad.khwab.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.BedtimeOff
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.ui.graphics.vector.ImageVector
import com.toblad.khwab.aura.model.TimePhase
import com.toblad.khwab.aura.model.WeatherState

/**
 * Provides contextual icon sets that reflect Aura's live
 * weather and time-of-day state.
 *
 * When Aura is inactive, all icons fall back to defaults.
 * When Aura is active, icons change to match the real-world
 * sky, weather, and position of sun/moon.
 */
object AuraIconProvider {

    data class HomeIcons(
        val mic: ImageVector,
        val stop: ImageVector,
        val chat: ImageVector,
        val settings: ImageVector,
        val start: ImageVector
    )

    /**
     * Returns icon set appropriate for the current Aura state.
     * Falls back to defaults when [auraActive] is false.
     */
    fun homeIcons(
        auraActive: Boolean,
        weather: WeatherState,
        timePhase: TimePhase
    ): HomeIcons {
        if (!auraActive) {
            return HomeIcons(
                mic = Icons.Default.Mic,
                stop = Icons.Default.StopCircle,
                chat = Icons.Default.Chat,
                settings = Icons.Default.Settings,
                start = Icons.Default.Mic
            )
        }

        val micIcon = micIconFor(weather, timePhase)

        return HomeIcons(
            mic = micIcon,
            stop = Icons.Default.StopCircle,
            chat = chatIconFor(timePhase),
            settings = settingsIconFor(timePhase),
            start = micIcon
        )
    }

    /**
     * Mic / voice icon: shifts with weather first, then time.
     * Storm → lightning bolt, Rain → water drop, Snow → snowflake,
     * Fog → wind, Cloudy night → moon, Clear → sun/moon position.
     */
    fun micIconFor(weather: WeatherState, timePhase: TimePhase): ImageVector {
        return when (weather) {
            WeatherState.STORM  -> Icons.Default.Thunderstorm
            WeatherState.RAIN   -> Icons.Default.WaterDrop
            WeatherState.SNOW   -> Icons.Default.AcUnit
            WeatherState.FOG    -> Icons.Default.Air
            WeatherState.CLOUDY -> when (timePhase) {
                TimePhase.NIGHT, TimePhase.MIDNIGHT -> Icons.Default.NightsStay
                else -> Icons.Default.WbCloudy
            }
            WeatherState.CLEAR  -> timeIcon(timePhase)
        }
    }

    /**
     * Chat icon reflects time of day.
     */
    fun chatIconFor(timePhase: TimePhase): ImageVector = when (timePhase) {
        TimePhase.NIGHT, TimePhase.MIDNIGHT    -> Icons.Default.NightsStay
        TimePhase.MORNING, TimePhase.PRE_DAWN  -> Icons.Default.WbSunny
        TimePhase.NOON                         -> Icons.Default.LightMode
        TimePhase.AFTERNOON, TimePhase.SUNRISE -> Icons.Default.WbTwilight
        TimePhase.EVENING, TimePhase.SUNSET    -> Icons.Default.WbTwilight
    }

    /**
     * Settings icon shifts with sky position — sun vs. star/moon.
     */
    fun settingsIconFor(timePhase: TimePhase): ImageVector = when (timePhase) {
        TimePhase.NIGHT, TimePhase.MIDNIGHT -> Icons.Default.Stars
        TimePhase.PRE_DAWN, TimePhase.SUNRISE -> Icons.Default.BedtimeOff
        else -> Icons.Default.Settings
    }

    private fun timeIcon(timePhase: TimePhase): ImageVector = when (timePhase) {
        TimePhase.PRE_DAWN  -> Icons.Default.DarkMode
        TimePhase.SUNRISE   -> Icons.Default.WbTwilight
        TimePhase.MORNING   -> Icons.Default.WbSunny
        TimePhase.NOON      -> Icons.Default.LightMode
        TimePhase.AFTERNOON -> Icons.Default.WbSunny
        TimePhase.SUNSET    -> Icons.Default.WbTwilight
        TimePhase.EVENING   -> Icons.Default.NightsStay
        TimePhase.NIGHT     -> Icons.Default.DarkMode
        TimePhase.MIDNIGHT  -> Icons.Default.NightsStay
    }
}
