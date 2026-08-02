package com.toblad.khwab.aura

import com.toblad.khwab.aura.api.AuraApi
import com.toblad.khwab.aura.manager.AuraManager
import com.toblad.khwab.aura.model.AuraConfig
import com.toblad.khwab.aura.model.AuraState
import com.toblad.khwab.aura.model.AuraTheme
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
    }

    fun refresh() {
        aura.refresh()
    }
}