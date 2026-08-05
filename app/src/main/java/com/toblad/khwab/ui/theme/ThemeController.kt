package com.toblad.khwab.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.toblad.khwab.aura.model.AuraTheme
import com.toblad.khwab.aura.model.ThemeProfile

object ThemeController {

    var currentTheme by mutableStateOf(ThemeMode.DEFAULT)
        private set

    /**
     * Colors derived from Aura's latest real-world profile
     * (sky, weather, ambient light). Recomputed by
     * [updateAuraProfile] whenever Aura refreshes with new
     * location, weather, or time data.
     */
    var currentAuraColors by mutableStateOf(auraColorScheme(AuraTheme().profile))
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
     * Recomputes the live Aura color scheme from a fresh
     * [ThemeProfile] reflecting real sky/weather/light
     * conditions.
     */
    fun updateAuraProfile(profile: ThemeProfile) {
        currentAuraColors = auraColorScheme(profile)
    }
}