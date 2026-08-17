package com.toblad.khwab.aura

import android.util.Log

/**
 * Static shim invoked by [AuraAndroidBridge.cs] via AndroidJavaClass.
 *
 * Unity calls:
 *   AndroidJavaClass("com.toblad.khwab.aura.UnityAuraBridgeCallback").CallStatic("onUnityReady")
 *   AndroidJavaClass("com.toblad.khwab.aura.UnityAuraBridgeCallback").CallStatic("onAuraActivated")
 *   AndroidJavaClass("com.toblad.khwab.aura.UnityAuraBridgeCallback").CallStatic("onAuraDeactivated")
 *   AndroidJavaClass("com.toblad.khwab.aura.UnityAuraBridgeCallback").CallStatic("onUnityHeartbeat", ...)
 *
 * This class is intentionally thin — it simply forwards to [UnityAuraBridge].
 * Keeping this as a separate @JvmStatic entry point lets Unity reference a
 * stable fully-qualified class name without coupling to internal object details.
 *
 * NOTE: This class must NOT be renamed, moved, or obfuscated by R8/ProGuard.
 * It is referenced by its fully-qualified class name from Unity C# via JNI.
 * The keep rule in proguard-rules.pro protects it.
 */
object UnityAuraBridgeCallback {

    private const val TAG = "UnityAuraBridgeCB"
    private const val DIAG = "[AURA-CALLBACK]"

    /**
     * Called by [AuraAndroidBridge.cs] once Awake() has finished.
     * Invoked on the Unity thread — safe to forward because [UnityAuraBridge.onUnityReady]
     * marshals its queue flush through the Android main-thread handler.
     */
    @JvmStatic
    fun onUnityReady() {
        Log.i(TAG, "$DIAG onUnityReady() — JNI call reached Android successfully (thread=${Thread.currentThread().name})")
        UnityAuraBridge.onUnityReady()
    }

    /**
     * Called by [AuraLifecycleController.cs] via [AuraAndroidBridge] when the
     * Aura fade-in completes and the environment is fully ACTIVE.
     *
     * Invoked on the Unity thread.
     */
    @JvmStatic
    fun onAuraActivated() {
        Log.i(TAG, "$DIAG onAuraActivated() — JNI call reached Android successfully (thread=${Thread.currentThread().name})")
        UnityAuraBridge.onAuraActivated()
    }

    /**
     * Called by [AuraLifecycleController.cs] via [AuraAndroidBridge] when the
     * Aura fade-out completes and the environment is fully OFF.
     *
     * Invoked on the Unity thread.
     */
    @JvmStatic
    fun onAuraDeactivated() {
        Log.i(TAG, "$DIAG onAuraDeactivated() — JNI call reached Android successfully (thread=${Thread.currentThread().name})")
        UnityAuraBridge.onAuraDeactivated()
    }

    /**
     * Periodic diagnostic heartbeat from [AuraAndroidBridge.cs] Update().
     * Carries full Unity-side diagnostic state so the debug console can show it
     * on-device without ADB.
     *
     * Invoked on the Unity thread every ~3 seconds once the JNI class is loadable.
     *
     * @param awakeStarted    True if AuraAndroidBridge.Awake() started.
     * @param awakeCompleted  True if AuraAndroidBridge.Awake() completed.
     * @param jniClassOk      True if AndroidJavaClass loaded successfully.
     * @param callStaticOk    True if CallStatic("onUnityReady") succeeded.
     * @param auraState       Unity AuraState enum name ("Off","Starting","Active","Stopping","NULL").
     * @param lastJniError    Most recent JNI exception type:message, or "".
     * @param retryCount      Number of NotifyAndroidReady() retry attempts.
     */
    @JvmStatic
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
        UnityAuraBridge.onUnityHeartbeat(
            awakeStarted   = awakeStarted,
            awakeCompleted = awakeCompleted,
            jniClassOk     = jniClassOk,
            callStaticOk   = callStaticOk,
            auraState      = auraState,
            lastJniError   = lastJniError,
            retryCount     = retryCount
        )
    }
}
