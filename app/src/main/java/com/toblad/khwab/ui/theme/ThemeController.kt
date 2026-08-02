package com.toblad.khwab.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Controls the active visual theme used by Khwab.
 *
 * This controller acts as the bridge between
 * Khwab and the Aura module.
 *
 * Current Flow:
 *
 * Default Theme
 *        │
 *        ▼
 * ThemeController
 *        │
 *        ▼
 * Aura Theme
 *
 * Future:
 *
 * Voice Command
 *        │
 *        ▼
 * AuraManager
 *        │
 *        ▼
 * ThemeController.enableAura()
 */
object ThemeController {

    /**
     * Current theme mode.
     *
     * Compose automatically recomposes whenever
     * this value changes.
     */
    var currentTheme by mutableStateOf(ThemeMode.DEFAULT)
        private set

    /**
     * Returns true if Aura is active.
     */
    val isAuraEnabled: Boolean
        get() = currentTheme == ThemeMode.AURA

    /**
     * Enable Aura.
     */
    fun enableAura() {
        currentTheme = ThemeMode.AURA
    }

    /**
     * Disable Aura.
     */
    fun disableAura() {
        currentTheme = ThemeMode.DEFAULT
    }

    /**
     * Toggle Aura.
     */
    fun toggleAura() {
        currentTheme =
            if (currentTheme == ThemeMode.DEFAULT) {
                ThemeMode.AURA
            } else {
                ThemeMode.DEFAULT
            }
    }
}