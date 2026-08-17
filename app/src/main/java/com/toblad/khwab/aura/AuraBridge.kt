package com.toblad.khwab.aura

import android.content.Context
import android.util.Log
import com.toblad.khwab.aura.model.AuraConfig
import com.toblad.khwab.aura.model.AuraState
import com.toblad.khwab.aura.model.AuraTheme
import com.toblad.khwab.aura.model.TimePhase
import com.toblad.khwab.aura.model.WeatherState
import com.toblad.khwab.environment.AuraEnvironmentSync
import com.toblad.khwab.environment.AuraSyncScheduler
import com.toblad.khwab.ui.theme.ThemeController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "AuraBridge"
private const val DIAG = "[AURA-ANDROID]"

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
 * Android-side controller for the Unity Khwab Aura.
 *
 * This is the only place inside the Android app that drives Aura
 * activation/deactivation, config persistence, and environment sync.
 *
 * All rendering is delegated to Unity via [UnityAuraBridge].
 * The old 2D AuraManager/AuraApi dependency has been removed.
 *
 * Every entry point — voice command, Settings screen, app-relaunch
 * restore — must route through this object so behaviour is identical.
 */
object AuraBridge {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var configStore: AuraConfigStore? = null
    private var environmentSync: AuraEnvironmentSync? = null
    private var syncScheduler: AuraSyncScheduler? = null
    private var ambientSound: AmbientSoundController? = null

    @Volatile private var _config = AuraConfig()
    @Volatile private var _auraState = AuraState.OFF

    /**
     * The current time phase used to populate [AuraTheme].
     * Derived from the device clock on each theme push.
     */
    @Volatile private var _timePhase = TimePhase.MORNING
    @Volatile private var _weatherState = WeatherState.CLEAR

    @Volatile private var initialized = false

    private val _snapshotFlow = MutableStateFlow(
        AuraSnapshot(AuraTheme(), AuraConfig())
    )

    /**
     * Live combined theme + config state. Collect this instead
     * of polling — it emits exactly when something real changes.
     */
    val snapshotFlow: StateFlow<AuraSnapshot> = _snapshotFlow.asStateFlow()

    /**
     * Live [AuraState] flow — emits every time the lifecycle state changes.
     * Compose UI should collect this (or observe [ThemeController.currentAuraTheme].auraState)
     * rather than polling [getState()].
     */
    private val _auraStateFlow = MutableStateFlow(AuraState.OFF)
    val auraStateFlow: StateFlow<AuraState> = _auraStateFlow.asStateFlow()

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Loads saved preferences, resumes Aura if it was left on,
     * and prepares the background sync/sound components.
     *
     * Call once at app startup before Aura is used — see MainActivity.onCreate.
     * Safe to call more than once; only the first call has effect.
     */
    fun initialize(context: Context) {
        if (initialized) return
        initialized = true

        val app = context.applicationContext

        configStore = AuraConfigStore(app)
        environmentSync = AuraEnvironmentSync(app)
        syncScheduler = AuraSyncScheduler(app)
        ambientSound = AmbientSoundController(app)

        // Restore saved user preferences.
        val saved = configStore!!.applySaved(AuraConfig())
        _config = saved

        // Restore activation state: if Aura was left enabled, re-activate.
        // Unity may not be ready yet — UnityAuraBridge queues the command.
        if (saved.enabled) {
            setAuraState(AuraState.STARTING)
            ThemeController.enableAura()
            startBackgroundSync()
            Log.i(TAG, "$DIAG initialize: restored enabled=true → state=STARTING, queuing ActivateAura")
            UnityAuraBridge.activate()   // queued until Unity is ready
        } else {
            setAuraState(AuraState.OFF)
            ThemeController.disableAura()
            Log.i(TAG, "$DIAG initialize: saved enabled=false → state=OFF")
        }

        pushSnapshot()
    }

    /**
     * Activates Unity Aura:
     * - Marks Aura as STARTING
     * - Enables ThemeController (transparent Compose background)
     * - Persists enabled=true
     * - Starts weather/location background sync
     * - Sends ActivateAura to Unity (queued if Unity isn't ready yet)
     */
    fun activate() {
        _config = _config.copy(enabled = true)
        setAuraState(AuraState.STARTING)
        configStore?.save(_config)
        ThemeController.enableAura()
        startBackgroundSync()
        pushSnapshot()
        Log.i(TAG, "$DIAG activate() → state=STARTING, dispatching ActivateAura to Unity")
        UnityAuraBridge.activate()
    }

    /**
     * Deactivates Unity Aura:
     * - Marks Aura as STOPPING (keeps ThemeMode.AURA so Unity can fade out over the transparent bg)
     * - Persists enabled=false
     * - Stops background sync
     * - Sends DeactivateAura to Unity
     * - ThemeController.disableAura() is called later in [onUnityAuraDeactivated]
     */
    fun deactivate() {
        _config = _config.copy(enabled = false)
        configStore?.save(_config)
        setAuraState(AuraState.STOPPING)
        stopBackgroundSync()
        pushSnapshot()
        UnityAuraBridge.deactivate()
        Log.d(TAG, "deactivate() called")
    }

    fun toggle() {
        if (isActive()) deactivate() else activate()
    }

    /** Returns true when Aura is enabled (active or starting). */
    fun isActive(): Boolean =
        _auraState == AuraState.ACTIVE || _auraState == AuraState.STARTING

    fun getState(): AuraState = _auraState

    /**
     * Called by [UnityAuraBridge] when Unity's [AuraLifecycleController] signals
     * that the fade-in is complete.  Advances Android state from STARTING → ACTIVE.
     *
     * Must be called on the Android main thread (guaranteed by [UnityAuraBridge]).
     */
    fun onUnityAuraActivated() {
        Log.i(TAG, "$DIAG onUnityAuraActivated() received from Unity — current state=$_auraState")
        if (_auraState != AuraState.STARTING) {
            Log.w(TAG, "$DIAG onUnityAuraActivated() — unexpected state $_auraState, ignoring")
            return
        }
        setAuraState(AuraState.ACTIVE)
        pushSnapshot()
        Log.i(TAG, "$DIAG onUnityAuraActivated() → state=ACTIVE ✓")
    }

    /**
     * Called by [UnityAuraBridge] when Unity's [AuraLifecycleController] signals
     * that the fade-out is complete.  Advances Android state from STOPPING → OFF and
     * restores the normal Khwab theme.
     *
     * Must be called on the Android main thread (guaranteed by [UnityAuraBridge]).
     */
    fun onUnityAuraDeactivated() {
        if (_auraState != AuraState.STOPPING) {
            Log.d(TAG, "onUnityAuraDeactivated() — unexpected state $_auraState, ignoring")
            return
        }
        setAuraState(AuraState.OFF)
        ThemeController.disableAura()
        pushSnapshot()
        Log.d(TAG, "onUnityAuraDeactivated() → OFF, theme restored")
    }

    /**
     * Marks Aura as ERROR.  Called when Unity cannot initialise or reports a
     * critical failure.  Restores the normal Khwab theme so the UI does not
     * remain permanently transparent.
     */
    fun onUnityAuraError(reason: String) {
        Log.e(TAG, "onUnityAuraError: $reason")
        setAuraState(AuraState.ERROR)
        ThemeController.disableAura()
        pushSnapshot()
    }

    /** Returns the current Aura theme snapshot. */
    fun getTheme(): AuraTheme = _snapshotFlow.value.theme

    fun getConfig(): AuraConfig = _config

    /**
     * Updates Aura config and persists user-facing preference fields.
     * Location and storm intensity are not persisted — they are live values.
     */
    fun updateConfig(config: AuraConfig) {
        _config = config
        configStore?.save(config)
        // Sync enabled state with activation
        if (config.enabled && !isActive()) {
            activate()
            return
        } else if (!config.enabled && isActive()) {
            deactivate()
            return
        }
        pushSnapshot()
    }

    /**
     * Supplies Aura with fresh real-world weather.
     * Forwards weather to Unity so it can update its weather system.
     */
    fun updateWeather(weather: WeatherState) {
        _weatherState = weather
        pushSnapshot()
        if (isActive()) {
            UnityAuraBridge.setWeather(weather)
        }
    }

    /**
     * Supplies Aura with the device's real coordinates.
     */
    fun updateLocation(latitude: Double, longitude: Double) {
        _config = _config.copy(latitude = latitude, longitude = longitude)
        pushSnapshot()
        if (isActive()) {
            UnityAuraBridge.setLocation(latitude, longitude)
        }
    }

    /**
     * Supplies a storm severity score (0.0–1.0).
     */
    fun updateStormIntensity(intensity: Float) {
        _config = _config.copy(stormIntensity = intensity.coerceIn(0f, 1f))
        pushSnapshot()
    }

    fun refresh() {
        pushSnapshot()
        if (isActive()) {
            val lat = _config.latitude
            val lon = _config.longitude
            if (lat != null && lon != null) {
                UnityAuraBridge.setLocation(lat, lon)
            }
            UnityAuraBridge.setWeather(_weatherState)
            UnityAuraBridge.syncRealTime()
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun setAuraState(state: AuraState) {
        _auraState = state
        _auraStateFlow.value = state
    }

    private fun startBackgroundSync() {
        environmentSync?.hydrateFromCache()
        scope.launch {
            try { environmentSync?.sync() } catch (_: Exception) {}
        }
        syncScheduler?.start()
        ambientSound?.start()
    }

    private fun stopBackgroundSync() {
        syncScheduler?.stop()
        ambientSound?.stop()
    }

    /**
     * Builds an [AuraTheme] from current Android-side state and emits
     * it via [snapshotFlow]. Also notifies [ThemeController].
     *
     * The time phase is derived from the device clock so the Compose UI
     * has an approximate phase for icon/color selection even before Unity
     * reports its own state.
     */
    private fun pushSnapshot() {
        val timePhase = currentTimePhase()
        _timePhase = timePhase

        val theme = AuraTheme(
            auraState = _auraState,
            timePhase = timePhase,
            weatherState = _weatherState,
            enabled = _config.enabled,
            animationsEnabled = _config.animationsEnabled,
            solarElevNorm = currentSolarElevNorm(timePhase),
            isSolarAccurate = _config.latitude != null
        )

        ThemeController.updateAuraTheme(theme)
        _snapshotFlow.value = AuraSnapshot(theme, _config)
    }

    // ── Time-phase helpers ────────────────────────────────────────────────────

    /**
     * Derives an approximate [TimePhase] from the device's local clock.
     * Used for Android-side icon and color decisions only — Unity
     * independently computes accurate solar elevation from GPS.
     */
    private fun currentTimePhase(): TimePhase {
        val hour = java.util.Calendar.getInstance()
            .get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 4..5   -> TimePhase.PRE_DAWN
            6         -> TimePhase.SUNRISE
            in 7..10  -> TimePhase.MORNING
            in 11..13 -> TimePhase.NOON
            in 14..16 -> TimePhase.AFTERNOON
            in 17..18 -> TimePhase.SUNSET
            in 19..20 -> TimePhase.EVENING
            in 21..22 -> TimePhase.NIGHT
            else      -> TimePhase.MIDNIGHT
        }
    }

    /**
     * Approximate normalised solar elevation from [TimePhase].
     * Sufficient for Compose UI tint decisions.
     */
    private fun currentSolarElevNorm(phase: TimePhase): Float = when (phase) {
        TimePhase.MIDNIGHT  -> -1.0f
        TimePhase.PRE_DAWN  -> -0.5f
        TimePhase.SUNRISE   ->  0.0f
        TimePhase.MORNING   ->  0.5f
        TimePhase.NOON      ->  1.0f
        TimePhase.AFTERNOON ->  0.6f
        TimePhase.SUNSET    ->  0.0f
        TimePhase.EVENING   -> -0.2f
        TimePhase.NIGHT     -> -0.7f
    }
}
