package com.toblad.khwab.debug

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.toblad.khwab.aura.UnityAuraManager
import com.toblad.khwab.ui.theme.KhwabTheme

/**
 * Debug-only Activity that hosts the Aura Testing Console.
 *
 * This file lives in src/debug/ and is therefore compiled only
 * into debug builds. It does not exist in release APKs at all —
 * neither the class nor the manifest entry.
 *
 * Navigate here from HomeScreen when BuildConfig.DEBUG is true.
 *
 * IMPORTANT: Unity's FrameLayout must be kept attached while this Activity
 * is in the foreground.  Without these lifecycle hooks the Unity view would
 * be detached by MainActivity.onPause() and never reattached, causing:
 *   Unity attached: NO  in the debug console.
 */
class AuraDebugActivity : ComponentActivity() {

    private val tag = "AuraDebugActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KhwabTheme {
                AuraDebugConsole(
                    onBackClick = { finish() }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.i(tag, "[DIAG] onStart()")
        UnityAuraManager.start()
    }

    override fun onResume() {
        super.onResume()
        Log.i(tag, "[DIAG] onResume() — attaching Unity to AuraDebugActivity")
        UnityAuraManager.resume()
        UnityAuraManager.attachTo(this)
    }

    override fun onPause() {
        Log.i(tag, "[DIAG] onPause() — detaching Unity from AuraDebugActivity")
        UnityAuraManager.detachFrom(this)
        UnityAuraManager.pause()
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        Log.i(tag, "[DIAG] onStop()")
        UnityAuraManager.stop()
    }
}
