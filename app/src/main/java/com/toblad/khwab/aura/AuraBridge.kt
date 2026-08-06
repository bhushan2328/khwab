package com.toblad.khwab.aura

import android.content.Context
import com.toblad.khwab.aura.api.AuraApi
import com.toblad.khwab.aura.manager.AuraManager
import com.toblad.khwab.aura.model.AuraConfig
import com.toblad.khwab.aura.model.AuraState
import com.toblad.khwab.aura.model.AuraTheme
import com.toblad.khwab.aura.model.WeatherState
import com.toblad.khwab.environment.AuraEnvironmentSync
import com.toblad.khwab.environment.AuraSyncScheduler
import com.toblad.khwab.ui.theme.ThemeController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Single combined snapshot of Aura's live state, exposed as a
 * StateFlow so consumers (ambient sound, future UI observers)
 * can react instantly to real changes instead of polling.
 */
data class AuraSnapshot(
    val theme: AuraTheme,
    val config: AuraConfig
)

/**
 * Android bridge to the Aura library.
 *
 * This is the only place inside the Android app that
 * communicates directly with the Aura module, and now also
 * owns the background weather sync loop and ambient sound —
 * so every entry point (voice command, Settings screen,
 * app-relaunch resume) behaves identically.
 */
object AuraBridge {

    private val aura: AuraApi = AuraManager()

    private val scope = CoroutineScope(Dispatchers.Main)

    private var configStore: AuraConfigStore? = null
    private var environmentSync: AuraEnvironmentSync? = null
    private var syncScheduler: AuraSyncScheduler? = null
    private var ambientSound: AmbientSoundController? = null

    private var initialized = false

    private val _snapshotFlow =
        MutableStateFlow(AuraSnapshot(aura.getTheme(), aura.getConfig()))

    /**
     * Live combined theme + config state. Collect this instead
     * of polling getTheme()/getConfig() — it emits exactly
     * when something real changes.
     */
    val snapshotFlow: StateFlow<AuraSnapshot> = _snapshotFlow.asStateFlow()

    /**
     * Loads saved preferences, resumes Aura if it was left on,
     * and prepares the background sync/sound components.
     *
     * Call this once at app startup, before Aura is used — see
     * MainActivity.onCreate. Safe to call more than once; only
     * the first call has effect.
     */
    fun initialize(context: Context) {

        if (initialized) return
        initialized = true

        val app = context.applicationContext

        configStore = AuraConfigStore(app)
        environmentSync = AuraEnvironmentSync(app)
        syncScheduler = AuraSyncScheduler(app)
        ambientSound = AmbientSoundController(app)

        val restored = configStore!!.applySaved(aura.getConfig())

        aura.updateConfig(restored.copy(enabled = aura.getConfig().enabled))

        if (restored.enabled && !aura.isActive()) {
            aura.activate()
        } else if (!restored.enabled && aura.isActive()) {
            aura.deactivate()
        }

        if (aura.isActive()) {
            ThemeController.enableAura()
            startBackgroundSync()
        }

        pushTheme()
    }

    fun activate() {
        aura.activate()
        ThemeController.enableAura()
        configStore?.save(aura.getConfig())
        startBackgroundSync()
        pushTheme()
    }

    fun deactivate() {
        aura.deactivate()
        ThemeController.disableAura()
        configStore?.save(aura.getConfig())
        stopBackgroundSync()
        pushTheme()
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

    /**
     * Updates Aura's config and persists the user-facing
     * preference fields. Location and storm intensity are
     * deliberately not persisted — those are live values
     * refreshed independently by AuraEnvironmentSync.
     */
    fun updateConfig(config: AuraConfig) {
        aura.updateConfig(config)
        configStore?.save(config)
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

    /**
     * Supplies Aura with the device's real coordinates so it
     * can compute real sunrise/sunset, moon phase and season
     * for this location instead of fixed generic defaults.
     */
    fun updateLocation(latitude: Double, longitude: Double) {
        aura.updateConfig(
            aura.getConfig().copy(
                latitude = latitude,
                longitude = longitude
            )
        )
        pushTheme()
    }

    /**
     * Supplies Aura with a real-world storm severity score
     * (0.0–1.0), derived from live wind speed/precipitation.
     */
    fun updateStormIntensity(intensity: Float) {
        aura.updateConfig(
            aura.getConfig().copy(
                stormIntensity = intensity.coerceIn(0f, 1f)
            )
        )
        pushTheme()
    }

    fun refresh() {
        aura.refresh()
        pushTheme()
    }

    private fun startBackgroundSync() {
        environmentSync?.hydrateFromCache()
        scope.launch { environmentSync?.sync() }
        syncScheduler?.start()
        ambientSound?.start()
    }

    private fun stopBackgroundSync() {
        syncScheduler?.stop()
        ambientSound?.stop()
    }

    /**
     * Pushes the latest Aura profile into the UI theme layer
     * and republishes the combined snapshot for any collector
     * (ambient sound, future observers).
     */
    private fun pushTheme() {
        ThemeController.updateAuraProfile(aura.getTheme().profile)
        _snapshotFlow.value = AuraSnapshot(aura.getTheme(), aura.getConfig())
    }
}