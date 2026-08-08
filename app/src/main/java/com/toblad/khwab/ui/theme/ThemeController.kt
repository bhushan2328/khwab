package com.toblad.khwab.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.toblad.khwab.aura.model.AuraTheme

object ThemeController {

    var currentTheme by mutableStateOf(ThemeMode.DEFAULT)
        private set

    /**
     * Colors derived from Aura's latest real-world profile
     * (sky, weather, ambient light). Recomputed by
     * [updateAuraTheme] whenever Aura refreshes with new
     * location, weather, or time data.
     */
    var currentAuraColors by mutableStateOf(auraColorScheme(AuraTheme().profile))
        private set

    /**
     * Live Aura theme snapshot (weather, time phase, sun/moon).
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
     * Recomputes the live Aura color scheme and stores the
     * latest theme snapshot from a fresh [AuraTheme] reflecting
     * real sky/weather/light conditions.
     */
    fun updateAuraTheme(theme: AuraTheme) {
        currentAuraTheme = theme
        currentAuraColors = auraColorScheme(theme.profile)
    }
    // fix #8: removed updateAuraProfile — dead code that caused stale currentAuraTheme
    // if called (colors updated but theme snapshot lagged behind). Use updateAuraTheme().
}