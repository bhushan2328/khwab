package com.toblad.khwab.aura

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.toblad.khwab.aura.model.WeatherState
import com.unity3d.player.UnityPlayer
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Snapshot of the Android↔Unity bridge diagnostic state.
 * Emitted by [UnityAuraBridge.diagnosticFlow] whenever a key pipeline event occurs.
 * Consumed by [AuraDebugConsole] to show live pipeline status without polling.
 *
 * Fields added in v2: full Unity-side diagnostic state surfaced via the heartbeat
 * callback so the on-device debug console can display the exact JNI failure without ADB.
 */
data class AuraBridgeDiagnostic(
    val isUnityReady: Boolean = false,
    val lastCommandSent: String = "none",
    val lastCallbackReceived: String = "none",
    // ── Unity-side diagnostic fields (populated via onUnityHeartbeat) ─────────
    /** True once AuraAndroidBridge.Awake() has started on the Unity thread. */
    val unityAwakeStarted: Boolean = false,
    /** True once AuraAndroidBridge.Awake() has completed on the Unity thread. */
    val unityAwakeCompleted: Boolean = false,
    /** True once AndroidJavaClass("…UnityAuraBridgeCallback") succeeded. */
    val jniClassLoaded: Boolean = false,
    /** True once CallStatic("onUnityReady") succeeded (same as isUnityReady once set). */
    val jniCallStaticOk: Boolean = false,
    /** Current Unity-side AuraState name ("Off","Starting","Active","Stopping","NULL"). */
    val unityAuraState: String = "unknown",
    /** Most recent JNI exception type:message, or "" if no error. */
    val lastJniError: String = "",
    /** Number of NotifyAndroidReady() retry attempts from Unity Update(). */
    val jniRetryCount: Int = 0,
    /** True once the first heartbeat has arrived from Unity. */
    val heartbeatReceived: Boolean = false
)

/**
 * Android-side adapter responsible solely for sending commands from Khwab Android
 * to the Unity [AuraAndroidBridge] C# script via [UnityPlayer.UnitySendMessage].
 *
 * # Responsibilities
 * - Send Aura activation / deactivation commands to Unity.
 * - Forward environment data (location, weather, time) to Unity.
 * - Queue commands when Unity is not yet ready and flush them once it is.
 * - Nothing else. No lifecycle management. No UnityPlayer construction.
 *
 * # Unity contract
 * The Unity GameObject must be named exactly **"AuraAndroidBridge"** (enforced in
 * `AuraAndroidBridge.cs Awake()`). The following message entry points are used:
 *
 * | Method          | Parameter format                      |
 * |-----------------|---------------------------------------|
 * | ActivateAura    | "" (empty)                            |
 * | DeactivateAura  | "" (empty)                            |
 * | SetLocation     | "latitude,longitude"  e.g. "33.7,73.0"|
 * | SetWeather      | WeatherType name  e.g. "Rain"         |
 * | SetTimeOfDay    | decimal hours  e.g. "18.5"            |
 * | SyncRealTime    | "" (empty)                            |
 *
 * # Readiness contract
 * Commands received before Unity signals readiness are placed in [_pendingQueue].
 * Once [onUnityReady] is called — triggered by [AuraAndroidBridge.cs] via
 * [UnityAuraBridgeCallback] after its Awake() completes — the queue is flushed
 * in FIFO order via [UnityPlayer.UnitySendMessage].
 *
 * This is a reliable callback-driven mechanism with a 500 ms retry loop on the
 * Unity side (Update()) to handle classloader unavailability at Awake() time.
 *
 * The queue is thread-safe ([CopyOnWriteArrayList] with synchronized drain).
 * [onUnityReady] marshals to the Android main thread before flushing.
 *
 * [UnityAuraManager] is the sole owner of the UnityPlayer instance.
 * This class never creates or destroys a UnityPlayer.
 */
object UnityAuraBridge {

    private const val TAG = "UnityAuraBridge"
    private const val DIAG = "[AURA-BRIDGE]"

    /** Name of the Unity GameObject that hosts AuraAndroidBridge.cs. */
    private const val BRIDGE_OBJECT = "AuraAndroidBridge"

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * True once [onUnityReady] has been called by the Unity scripting runtime.
     * Protected by [_pendingQueue] monitor — only mutated inside synchronized blocks.
     */
    @Volatile private var _isReady = false

    /**
     * Commands queued while Unity has not yet signalled readiness.
     * Flushed in FIFO order once [onUnityReady] is called.
     * CopyOnWriteArrayList provides safe iteration; add/drain is synchronized.
     */
    private val _pendingQueue = CopyOnWriteArrayList<Pair<String, String>>()

    // ── Diagnostic state — observable for the debug console ──────────────────

    private val _diagnosticFlow = MutableStateFlow(AuraBridgeDiagnostic())

    /**
     * Live bridge diagnostic state.
     * Collect in [AuraDebugConsole] to get reactive pipeline status updates.
     */
    val diagnosticFlow: StateFlow<AuraBridgeDiagnostic> = _diagnosticFlow.asStateFlow()

    /** Whether Unity has signalled it is ready. */
    val isUnityReady: Boolean get() = _isReady

    /** Last command method sent or queued. Snapshot; use [diagnosticFlow] for reactivity. */
    val lastCommandSent: String get() = _diagnosticFlow.value.lastCommandSent

    /** Last callback method received from Unity. Snapshot; use [diagnosticFlow] for reactivity. */
    val lastCallbackReceived: String get() = _diagnosticFlow.value.lastCallbackReceived

    private fun emitDiagnostic(
        ready: Boolean = _isReady,
        command: String = _diagnosticFlow.value.lastCommandSent,
        callback: String = _diagnosticFlow.value.lastCallbackReceived,
        awakeStarted: Boolean    = _diagnosticFlow.value.unityAwakeStarted,
        awakeCompleted: Boolean  = _diagnosticFlow.value.unityAwakeCompleted,
        jniClassLoaded: Boolean  = _diagnosticFlow.value.jniClassLoaded,
        jniCallStaticOk: Boolean = _diagnosticFlow.value.jniCallStaticOk,
        unityAuraState: String   = _diagnosticFlow.value.unityAuraState,
        lastJniError: String     = _diagnosticFlow.value.lastJniError,
        jniRetryCount: Int       = _diagnosticFlow.value.jniRetryCount,
        heartbeatReceived: Boolean = _diagnosticFlow.value.heartbeatReceived
    ) {
        _diagnosticFlow.value = AuraBridgeDiagnostic(
            isUnityReady       = ready,
            lastCommandSent    = command,
            lastCallbackReceived = callback,
            unityAwakeStarted  = awakeStarted,
            unityAwakeCompleted = awakeCompleted,
            jniClassLoaded     = jniClassLoaded,
            jniCallStaticOk    = jniCallStaticOk,
            unityAuraState     = unityAuraState,
            lastJniError       = lastJniError,
            jniRetryCount      = jniRetryCount,
            heartbeatReceived  = heartbeatReceived
        )
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Tells Unity to activate the Aura environment.
     * Unity side: [AuraAndroidBridge.ActivateAura] → [AuraLifecycleController.ActivateAura].
     */
    fun activate() {
        Log.i(TAG, "$DIAG activate() called — isReady=$_isReady, queueSize=${_pendingQueue.size}")
        emitDiagnostic(command = "ActivateAura")
        send("ActivateAura", "")
    }

    /**
     * Tells Unity to deactivate the Aura environment.
     * Unity side: [AuraAndroidBridge.DeactivateAura] → [AuraLifecycleController.DeactivateAura].
     */
    fun deactivate() {
        send("DeactivateAura", "")
    }

    /**
     * Forwards the device's GPS coordinates to Unity so it can compute accurate
     * sunrise/sunset and solar elevation for this location.
     *
     * Unity side: [AuraAndroidBridge.SetLocation] → [EnvironmentClock.Latitude/Longitude].
     *
     * @param latitude  Decimal degrees north (+) / south (−).
     * @param longitude Decimal degrees east (+) / west (−).
     */
    fun setLocation(latitude: Double, longitude: Double) {
        val payload = String.format(Locale.US, "%.6f,%.6f", latitude, longitude)
        send("SetLocation", payload)
    }

    /**
     * Forwards the current weather to Unity so it can update its weather system.
     *
     * Android [WeatherState] values are mapped to the Unity [WeatherType] names
     * accepted by [AuraAndroidBridge.SetWeather]:
     *
     * | Android WeatherState | Unity WeatherType |
     * |----------------------|-------------------|
     * | CLEAR                | Clear             |
     * | CLOUDY               | Cloudy            |
     * | RAIN                 | Rain              |
     * | SNOW                 | Snow              |
     * | FOG                  | Fog               |
     * | STORM                | Storm             |
     *
     * Unity side: [AuraAndroidBridge.SetWeather] → [WeatherSystem.SetWeather].
     */
    fun setWeather(weather: WeatherState) {
        val unityWeatherName = when (weather) {
            WeatherState.CLEAR  -> "Clear"
            WeatherState.CLOUDY -> "Cloudy"
            WeatherState.RAIN   -> "Rain"
            WeatherState.SNOW   -> "Snow"
            WeatherState.FOG    -> "Fog"
            WeatherState.STORM  -> "Storm"
        }
        send("SetWeather", unityWeatherName)
    }

    /**
     * Overrides Unity's time of day (disables real-time sync on the Unity side).
     * Call [syncRealTime] to re-enable real-time sync.
     *
     * Unity side: [AuraAndroidBridge.SetTimeOfDay] → [EnvironmentClock.TimeOfDay].
     *
     * @param hoursDecimal Time as decimal hours 0.0–23.99  (e.g. 18.5 = 18:30).
     */
    fun setTimeOfDay(hoursDecimal: Float) {
        val payload = String.format(Locale.US, "%.4f", hoursDecimal)
        send("SetTimeOfDay", payload)
    }

    /**
     * Re-enables real-time clock sync on the Unity side (undoes a [setTimeOfDay] override).
     *
     * Unity side: [AuraAndroidBridge.SyncRealTime] → [EnvironmentClock.SyncWithRealTime = true].
     */
    fun syncRealTime() {
        send("SyncRealTime", "")
    }

    /**
     * Called by [UnityAuraBridgeCallback] when [AuraAndroidBridge.cs] Awake() completes.
     * This is the authoritative signal that [UnityPlayer.UnitySendMessage] can now reach
     * the "AuraAndroidBridge" GameObject reliably.
     *
     * - Marks Unity as ready ([_isReady] = true).
     * - Flushes all queued commands in FIFO order.
     * - Marshalled to the Android main thread so the flush is always on the same thread
     *   that [send] runs on, preventing a race between a concurrent [send] and the drain.
     *
     * Idempotent: subsequent calls after the first are no-ops.
     */
    fun onUnityReady() {
        Log.i(TAG, "$DIAG onUnityReady() received from Unity scripting runtime (thread=${Thread.currentThread().name})")
        mainHandler.post {
            if (_isReady) {
                Log.i(TAG, "$DIAG onUnityReady() — already ready, ignoring duplicate call")
                return@post
            }
            _isReady = true
            emitDiagnostic(ready = true, callback = "onUnityReady", jniCallStaticOk = true)
            val count = _pendingQueue.size
            Log.i(TAG, "$DIAG onUnityReady() — _isReady=true, flushing $count queued command(s)")
            flushQueue()
            Log.i(TAG, "$DIAG onUnityReady() — queue flush complete")
        }
    }

    /**
     * Called by [UnityAuraBridgeCallback] when [AuraLifecycleController.cs] has finished
     * fading in (transition progress reached 1.0).  Forwarded to [AuraBridge] so the
     * Android-side state machine can advance from STARTING → ACTIVE.
     *
     * Marshalled to the Android main thread.
     */
    fun onAuraActivated() {
        Log.i(TAG, "$DIAG onAuraActivated() received from Unity (thread=${Thread.currentThread().name})")
        emitDiagnostic(callback = "onAuraActivated")
        mainHandler.post {
            Log.i(TAG, "$DIAG onAuraActivated() → forwarding to AuraBridge on main thread")
            AuraBridge.onUnityAuraActivated()
        }
    }

    /**
     * Called by [UnityAuraBridgeCallback] when [AuraLifecycleController.cs] has finished
     * fading out (transition progress reached 0.0).  Forwarded to [AuraBridge] so the
     * Android-side state machine can advance from STOPPING → OFF and restore the normal UI.
     *
     * Marshalled to the Android main thread.
     */
    fun onAuraDeactivated() {
        mainHandler.post {
            Log.d(TAG, "onAuraDeactivated() — Unity Aura fade-out complete")
            AuraBridge.onUnityAuraDeactivated()
        }
    }

    /**
     * Called by [UnityAuraBridgeCallback.onUnityHeartbeat] with the full Unity-side
     * diagnostic snapshot.  Updates [diagnosticFlow] so [AuraDebugConsole] can display
     * the exact JNI failure on-device without ADB.
     *
     * Invoked on the Unity thread — emits to the StateFlow directly (StateFlow.value
     * assignment is thread-safe).
     */
    fun onUnityHeartbeat(
        awakeStarted: Boolean,
        awakeCompleted: Boolean,
        jniClassOk: Boolean,
        callStaticOk: Boolean,
        auraState: String,
        lastJniError: String,
        retryCount: Int
    ) {
        Log.d(TAG, "$DIAG onUnityHeartbeat() — awake=$awakeStarted/$awakeCompleted" +
                   ", jni=$jniClassOk/$callStaticOk, state=$auraState" +
                   ", retries=$retryCount, err=${lastJniError.ifEmpty { "none" }}")
        emitDiagnostic(
            awakeStarted     = awakeStarted,
            awakeCompleted   = awakeCompleted,
            jniClassLoaded   = jniClassOk,
            jniCallStaticOk  = callStaticOk,
            unityAuraState   = auraState,
            lastJniError     = lastJniError,
            jniRetryCount    = retryCount,
            heartbeatReceived = true
        )
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    /**
     * Sends a message via [UnityPlayer.UnitySendMessage] or queues it if Unity has
     * not yet signalled readiness via [onUnityReady].
     *
     * Commands are never silently dropped. They are either sent immediately
     * (if [_isReady] is true) or placed in [_pendingQueue] for later delivery.
     */
    private fun send(method: String, param: String) {
        synchronized(_pendingQueue) {
            if (!_isReady) {
                _pendingQueue.add(method to param)
                Log.i(TAG, "$DIAG send() — Unity not ready, queued [$method, \"$param\"] — queue now has ${_pendingQueue.size} item(s)")
                return
            }
        }
        Log.i(TAG, "$DIAG send() — Unity ready, sending [$method, \"$param\"] immediately")
        // Unity is ready — flush any still-queued commands first to maintain FIFO order,
        // then send the new command.
        flushQueue()
        sendDirect(method, param)
    }

    /**
     * Drains [_pendingQueue] in FIFO order via [UnitySendMessage].
     * Synchronized to prevent concurrent flushes racing with new enqueues.
     */
    private fun flushQueue() {
        synchronized(_pendingQueue) {
            val iter = _pendingQueue.iterator()
            while (iter.hasNext()) {
                val (m, p) = iter.next()
                sendDirect(m, p)
            }
            _pendingQueue.clear()
        }
    }

    /**
     * Dispatches a single [UnityPlayer.UnitySendMessage] call.
     * Caller must ensure Unity is initialized before calling this.
     */
    private fun sendDirect(method: String, param: String) {
        try {
            Log.i(TAG, "$DIAG UnitySendMessage(\"$BRIDGE_OBJECT\", \"$method\", \"$param\") — calling now")
            UnityPlayer.UnitySendMessage(BRIDGE_OBJECT, method, param)
            Log.i(TAG, "$DIAG UnitySendMessage(\"$BRIDGE_OBJECT\", \"$method\", \"$param\") — returned without exception")
        } catch (e: Exception) {
            Log.e(TAG, "$DIAG UnitySendMessage($method) FAILED: ${e.javaClass.simpleName}: ${e.message}")
        }
    }
}
