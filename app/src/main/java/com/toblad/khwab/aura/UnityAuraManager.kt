package com.toblad.khwab.aura

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.FrameLayout
import com.unity3d.player.IUnityPlayerLifecycleEvents
import com.unity3d.player.UnityPlayer
import com.unity3d.player.UnityPlayerForActivityOrService

/**
 * Application-scoped singleton that owns the single [UnityPlayerForActivityOrService] instance
 * for the entire Khwab process.
 *
 * Responsibilities:
 * - Create [UnityPlayerForActivityOrService] exactly once, using the first Activity context.
 * - Attach / detach Unity's FrameLayout into whichever Activity is currently visible.
 * - Forward Android lifecycle events (pause / resume / start / stop) to Unity.
 * - Never destroy UnityPlayer on an Activity transition — only on application shutdown.
 *
 * # Initialization strategy
 * [UnityPlayerForActivityOrService] requires an Activity context — Application context is
 * insufficient (Unity accesses Window, WindowManager, and Activity state internally).
 * Therefore initialization is deferred to the first [attachTo] call and cannot happen
 * in Application.onCreate().
 *
 * # Unity readiness
 * Unity readiness is signalled by the Unity scripting runtime itself: [AuraAndroidBridge.cs]
 * calls [UnityAuraBridgeCallback.onUnityReady] at the end of its Awake() method, which
 * forwards to [UnityAuraBridge.onUnityReady]. This is a deterministic callback — no fixed
 * delay is needed, and the queue flush happens exactly when the bridge GameObject is ready.
 *
 * # View embedding
 * Unity owns a [FrameLayout] (obtained via [UnityPlayerForActivityOrService.getFrameLayout])
 * that wraps its rendering SurfaceView.  When attaching to an Activity, this FrameLayout is
 * inserted at index 0 in android.R.id.content — behind the existing Compose ComposeView —
 * so Unity renders beneath the Khwab Compose UI without replacing it.
 *
 * # Thread safety
 * All view/window operations are executed on the Android main thread via [mainHandler].
 * State fields are only mutated on the main thread.
 */
object UnityAuraManager : IUnityPlayerLifecycleEvents {

    private const val TAG = "UnityAuraManager"

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * The single UnityPlayer instance for this process.
     * Non-null once [initialize] has been called successfully.
     * Only mutated on the main thread.
     */
    private var player: UnityPlayerForActivityOrService? = null

    /**
     * The Activity whose content root currently holds Unity's FrameLayout.
     * Only mutated on the main thread.
     */
    private var attachedActivity: Activity? = null

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Returns true once [UnityPlayerForActivityOrService] has been created. */
    fun isInitialized(): Boolean = player != null

    /** Returns true if Unity's FrameLayout is currently attached to an Activity. */
    fun isAttached(): Boolean = attachedActivity != null

    /**
     * Creates [UnityPlayerForActivityOrService] using [activity] as its context.
     * Safe to call multiple times — subsequent calls are no-ops.
     *
     * Readiness is no longer polled via a fixed delay. Instead, [AuraAndroidBridge.cs]
     * calls [UnityAuraBridgeCallback.onUnityReady] at the end of its Awake() method,
     * which routes to [UnityAuraBridge.onUnityReady] to flush the command queue.
     *
     * Must be called on the main thread, or will be posted to it.
     *
     * @param activity  The first Activity that will host Unity.  Unity requires an Activity
     *                  context at construction time.
     */
    fun initialize(activity: Activity) {
        runOnMainThread {
            if (player != null) return@runOnMainThread
            // UnityPlayerForActivityOrService constructor boots the Unity runtime.
            // AuraAndroidBridge.Awake() will call back via UnityAuraBridgeCallback
            // once the scripting runtime is ready to receive UnitySendMessage.
            Log.i(TAG, "[DIAG] initialize() — constructing UnityPlayerForActivityOrService" +
                       " with Activity=${activity.javaClass.simpleName}" +
                       " (thread=${Thread.currentThread().name})")
            // Set currentActivity before construction so Unity's JNI bridge has the
            // correct host-app classloader from the first moment of native init.
            UnityPlayer.currentActivity = activity
            player = UnityPlayerForActivityOrService(activity, this@UnityAuraManager)
            Log.i(TAG, "[DIAG] UnityPlayerForActivityOrService constructed — waiting for Unity-ready callback")
        }
    }

    /**
     * Attaches Unity's FrameLayout to [activity]'s content root, at index 0 (behind Compose).
     *
     * - If Unity is not yet initialized, initializes it using [activity] first.
     * - If Unity is attached to a different Activity, detaches from that Activity first.
     * - Safe to call repeatedly with the same Activity.
     *
     * Call from Activity.onResume() or Activity.onStart().
     */
    fun attachTo(activity: Activity) {
        Log.i(TAG, "[DIAG] attachTo() requested — Activity=${activity.javaClass.simpleName}" +
                   ", isInitialized=${player != null}, currentAttached=${attachedActivity?.javaClass?.simpleName ?: "none"}")
        runOnMainThread {
            if (player == null) {
                initialize(activity)
                // Post attach after init so the player field is set when we read it.
                mainHandler.post { attachToInternal(activity) }
                return@runOnMainThread
            }
            attachToInternal(activity)
        }
    }

    /**
     * Removes Unity's FrameLayout from [activity]'s content root.
     * Safe to call when not attached, or with a different Activity than the attached one.
     *
     * Call from Activity.onPause() or Activity.onStop() — but only when you expect
     * no other Activity will immediately pick it up.
     */
    fun detachFrom(activity: Activity) {
        Log.i(TAG, "[DIAG] detachFrom() requested — Activity=${activity.javaClass.simpleName}" +
                   ", currentAttached=${attachedActivity?.javaClass?.simpleName ?: "none"}")
        runOnMainThread {
            if (attachedActivity !== activity) {
                Log.i(TAG, "[DIAG] detachFrom() — skipped (not attached to ${activity.javaClass.simpleName})")
                return@runOnMainThread
            }
            detachInternal(activity)
        }
    }

    /**
     * Pauses Unity rendering and audio.
     * Call from the host Activity's onPause().
     */
    fun pause() {
        runOnMainThread { player?.onPause() }
    }

    /**
     * Resumes Unity rendering and audio.
     * Call from the host Activity's onResume().
     */
    fun resume() {
        runOnMainThread { player?.onResume() }
    }

    /**
     * Notifies Unity that the Activity is starting (becoming visible).
     * Call from the host Activity's onStart().
     */
    fun start() {
        runOnMainThread { player?.onStart() }
    }

    /**
     * Notifies Unity that the Activity is stopping (no longer visible).
     * Call from the host Activity's onStop().
     */
    fun stop() {
        runOnMainThread { player?.onStop() }
    }

    /**
     * Forwards a window-focus event to the Unity native runtime.
     * Call from the host Activity's [android.app.Activity.onWindowFocusChanged].
     *
     * Unity's internal render-loop start gate requires at least one
     * `windowFocusChanged(true)` call before the first frame is rendered.
     * When Unity is embedded (not running as its own Activity), the host
     * Activity must forward this event manually.
     *
     * @param hasFocus true when the window gains focus, false when it loses it.
     */
    fun notifyWindowFocus(hasFocus: Boolean) {
        Log.i(TAG, "[DIAG] notifyWindowFocus($hasFocus) — forwarding to UnityPlayer (player=${player != null})")
        runOnMainThread { player?.windowFocusChanged(hasFocus) }
    }

    /**
     * Permanently destroys the [UnityPlayerForActivityOrService].
     * Call only when the entire application process is shutting down — never on a
     * single Activity finish().
     *
     * After destroy() returns, [isInitialized] is false and the manager cannot be
     * reused without calling [initialize] again.
     */
    fun destroy() {
        runOnMainThread {
            val p = player ?: return@runOnMainThread
            attachedActivity?.let { detachInternal(it) }
            p.destroy()
            player = null
        }
    }

    // -------------------------------------------------------------------------
    // IUnityPlayerLifecycleEvents
    // -------------------------------------------------------------------------

    /** Unity requested an unload (e.g. scripted Application.Unload). Detach view cleanly. */
    override fun onUnityPlayerUnloaded() {
        runOnMainThread {
            attachedActivity?.let { detachInternal(it) }
        }
    }

    /** Unity process has quit (e.g. Application.Quit from C#). Clean up references. */
    override fun onUnityPlayerQuitted() {
        runOnMainThread {
            attachedActivity = null
            player = null
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Core attach logic — must be called on the main thread with [player] non-null.
     */
    private fun attachToInternal(activity: Activity) {
        Log.i(TAG, "[DIAG] attachToInternal() — Activity=${activity.javaClass.simpleName}")
        val p = player ?: run {
            Log.e(TAG, "[DIAG] attachToInternal() — player is null, cannot attach")
            return
        }

        // Already attached to this exact Activity — nothing to do.
        if (attachedActivity === activity) {
            Log.i(TAG, "[DIAG] attachToInternal() — already attached to ${activity.javaClass.simpleName}, skipping")
            return
        }

        // Detach from a previous Activity before attaching to the new one.
        attachedActivity?.let {
            Log.i(TAG, "[DIAG] attachToInternal() — detaching from previous ${it.javaClass.simpleName} before attaching to ${activity.javaClass.simpleName}")
            detachInternal(it)
        }

        // Keep Unity's currentActivity pointing at the foreground Activity so that
        // Unity's internal AndroidJavaClass class-loader resolution (which calls
        // currentActivity.getClassLoader()) uses the host app's class loader and
        // can find com.toblad.khwab.* classes at JNI call-back time.
        // Without this, Unity's native thread may have a stale or null currentActivity
        // and AndroidJavaClass("com.toblad.khwab.aura.UnityAuraBridgeCallback") throws
        // a ClassNotFoundException that is silently swallowed in NotifyAndroidReady().
        UnityPlayer.currentActivity = activity
        Log.i(TAG, "[DIAG] attachToInternal() — UnityPlayer.currentActivity set to ${activity.javaClass.simpleName}")

        // Unity's own FrameLayout is the correct root to embed.
        // It contains the rendering SurfaceView plus any splash/overlay views Unity needs.
        val unityFrame: FrameLayout = p.frameLayout

        // android.R.id.content is the root FrameLayout that Compose's ComposeView lives in.
        val contentRoot = activity.findViewById<FrameLayout>(android.R.id.content)

        // Guard: already in this container (e.g. re-entrant call).
        if (unityFrame.parent === contentRoot) {
            Log.i(TAG, "[DIAG] attachToInternal() — unityFrame already in contentRoot of ${activity.javaClass.simpleName}")
            attachedActivity = activity
            return
        }

        // Remove from any previous parent to avoid "already has a parent" IllegalStateException.
        (unityFrame.parent as? FrameLayout)?.removeView(unityFrame)

        // Insert at index 0 — behind the existing Compose ComposeView at index 1+.
        contentRoot.addView(
            unityFrame,
            0,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        Log.i(TAG, "[DIAG] attachToInternal() — unityFrame added to contentRoot of ${activity.javaClass.simpleName} at index 0")

        // Request focus on the Unity FrameLayout so the native render loop receives
        // a Surface with focus.  Without this, UnityPlayerForActivityOrService keeps
        // its internal focus gate closed and the Unity main loop never ticks —
        // equivalent to what UnityPlayerActivity.onCreate() does explicitly:
        //   mUnityPlayer.getFrameLayout().requestFocus()
        unityFrame.requestFocus()
        Log.i(TAG, "[DIAG] attachToInternal() — unityFrame.requestFocus() called")

        // Forward a synthetic window-focus-gained event so Unity's native runtime
        // treats the embedded view as focused.  Required when unity.run-without-focus
        // is false (the default); also harmless when it is true.
        p.windowFocusChanged(true)
        Log.i(TAG, "[DIAG] attachToInternal() — windowFocusChanged(true) forwarded to UnityPlayer")

        attachedActivity = activity
    }

    /**
     * Core detach logic — must be called on the main thread.
     */
    private fun detachInternal(activity: Activity) {
        Log.i(TAG, "[DIAG] detachInternal() — Activity=${activity.javaClass.simpleName}")
        val p = player
        val unityFrame: FrameLayout? = p?.frameLayout

        val contentRoot = activity.findViewById<FrameLayout>(android.R.id.content)
        if (unityFrame != null && unityFrame.parent === contentRoot) {
            contentRoot.removeView(unityFrame)
            Log.i(TAG, "[DIAG] detachInternal() — unityFrame removed from contentRoot of ${activity.javaClass.simpleName}")
        } else {
            Log.i(TAG, "[DIAG] detachInternal() — unityFrame not in contentRoot of ${activity.javaClass.simpleName} (parent=${unityFrame?.parent})")
        }

        attachedActivity = null
    }

    /**
     * Executes [block] on the Android main thread.
     * If already on the main thread, runs synchronously; otherwise posts via [mainHandler].
     */
    private fun runOnMainThread(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }
}
