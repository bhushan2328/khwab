package com.toblad.khwab.aura

import com.toblad.khwab.aura.api.AuraApi
import com.toblad.khwab.aura.manager.AuraManager
import com.toblad.khwab.aura.model.AuraConfig
import com.toblad.khwab.aura.model.AuraState
import com.toblad.khwab.aura.model.AuraTheme
import com.toblad.khwab.aura.model.WeatherState
import com.toblad.khwab.ui.theme.ThemeController

/**
 * Android bridge to the Aura library.
 *
 * This is the only place inside the Android app
 * that communicates directly with the Aura module.
 */
object AuraBridge {

    private val aura: AuraApi = AuraManager()

    fun activate() {
        aura.activate()
        ThemeController.enableAura()
        pushTheme()
    }

    fun deactivate() {
        aura.deactivate()
        ThemeController.disableAura()
    }

    fun toggle() {
        if (aura.isActive()) {
            deactivate()
        } else {
            activate()
        }
    }

    fun isActive(): Boolean =
        aura.isActive()

    fun getState(): AuraState =
        aura.getState()

    fun getTheme(): AuraTheme =
        aura.getTheme()

    fun getConfig(): AuraConfig =
        aura.getConfig()

    fun updateConfig(config: AuraConfig) {
        aura.updateConfig(config)
        pushTheme()
    }

    /**
     * Supplies Aura with fresh, real-world weather (fetched
     * from the device's actual location) so the next
     * generated theme reflects real conditions.
     */
    fun updateWeather(weather: WeatherState) {
        aura.updateWeather(weather)
        pushTheme()
    }

    fun refresh() {
        aura.refresh()
        pushTheme()
    }

    /**
     * Pushes the latest Aura profile into the UI theme layer
     * so any active screen recomposes with real conditions.
     */
    private fun pushTheme() {
        ThemeController.updateAuraProfile(aura.getTheme().profile)
    }
}