package com.toblad.khwab.aura

import android.util.Log

/**
 * Static shim invoked by [AuraAndroidBridge.cs] via AndroidJavaClass when the Unity
 * scripting runtime has completed [AuraAndroidBridge.Awake()] and is ready to receive
 * [UnityPlayer.UnitySendMessage] calls.
 *
 * Unity calls:
 *   AndroidJavaClass("com.toblad.khwab.aura.UnityAuraBridgeCallback").CallStatic("onUnityReady")
 *
 * This class is intentionally thin — it simply forwards to [UnityAuraBridge.onUnityReady].
 * Keeping this as a separate @JvmStatic entry point lets the Unity C# code reference a
 * stable fully-qualified class name without coupling directly to the object internals.
 */
object UnityAuraBridgeCallback {

    private const val TAG = "UnityAuraBridgeCB"

    /**
     * Called by [AuraAndroidBridge.cs] via AndroidJavaClass once Awake() has finished.
     * Invoked on the Unity thread — safe to forward because [UnityAuraBridge.onUnityReady]
     * marshals its queue flush through the Android main-thread handler.
     */
    @JvmStatic
    fun onUnityReady() {
        Log.d(TAG, "onUnityReady() received from Unity scripting runtime")
        UnityAuraBridge.onUnityReady()
    }
}
