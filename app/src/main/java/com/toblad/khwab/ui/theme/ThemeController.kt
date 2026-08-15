package com.toblad.khwab.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.toblad.khwab.aura.model.AuraTheme
import com.toblad.khwab.aura.model.WeatherState
import com.toblad.khwab.aura.model.TimePhase

object ThemeController {

    var currentTheme by mutableStateOf(ThemeMode.DEFAULT)
        private set

    /**
     * Colors derived from Aura's current weather + time of day.
     * Recomputed by [updateAuraTheme] whenever AuraBridge pushes new state.
     */
    var currentAuraColors by mutableStateOf(defaultAuraColors())
        private set

    /**
     * Live Aura theme snapshot (weather, time phase, aura state).
     * Updated every time AuraBridge pushes a new theme so that
     * any composable reading this state recomposes automatically.
     */
    var currentAuraTheme by mutableStateOf(AuraTheme())
        private set

    fun setTheme(theme: ThemeMode) {
        currentTheme = theme
    }

    fun toggleTheme() {
        currentTheme =
            if (currentTheme == ThemeMode.DEFAULT) {
                ThemeMode.AURA
            } else {
                ThemeMode.DEFAULT
            }
    }

    fun enableAura() {
        currentTheme = ThemeMode.AURA
    }

    fun disableAura() {
        currentTheme = ThemeMode.DEFAULT
    }

    /**
     * Recomputes the live Aura color scheme from [theme]'s weather and time phase,
     * and stores the latest theme snapshot for Compose observability.
     */
    fun updateAuraTheme(theme: AuraTheme) {
        currentAuraTheme = theme
        currentAuraColors = auraColorScheme(theme.weatherState, theme.timePhase)
    }

    /**
     * Returns a default Aura color scheme (morning / clear) used before the
     * first theme push arrives from AuraBridge.
     */
    private fun defaultAuraColors() =
        auraColorScheme(WeatherState.CLEAR, TimePhase.MORNING)
}
