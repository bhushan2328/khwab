package com.toblad.khwab.aura

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.toblad.khwab.aura.model.WeatherState
import com.unity3d.player.UnityPlayer
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

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
 * This is a reliable callback-driven mechanism. It replaces the previous
 * heuristic 1 000 ms postDelayed approach in UnityAuraManager.
 *
 * The queue is thread-safe ([CopyOnWriteArrayList] with synchronized drain).
 * [onUnityReady] marshals to the Android main thread before flushing.
 *
 * [UnityAuraManager] is the sole owner of the UnityPlayer instance.
 * This class never creates or destroys a UnityPlayer.
 */
object UnityAuraBridge {

    private const val TAG = "UnityAuraBridge"

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

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Tells Unity to activate the Aura environment.
     * Unity side: [AuraAndroidBridge.ActivateAura] → [AuraLifecycleController.ActivateAura].
     */
    fun activate() {
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
        mainHandler.post {
            if (_isReady) {
                Log.d(TAG, "onUnityReady() called again — already ready, ignoring")
                return@post
            }
            _isReady = true
            Log.d(TAG, "onUnityReady() — Unity ready, flushing ${_pendingQueue.size} queued commands")
            flushQueue()
        }
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
                Log.d(TAG, "Queued [$method, \"$param\"] (Unity not yet ready)")
                return
            }
        }
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
            UnityPlayer.UnitySendMessage(BRIDGE_OBJECT, method, param)
            Log.d(TAG, "UnitySendMessage(\"$BRIDGE_OBJECT\", \"$method\", \"$param\")")
        } catch (e: Exception) {
            Log.e(TAG, "UnitySendMessage($method) failed: ${e.message}")
        }
    }
}
