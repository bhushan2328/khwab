package com.toblad.khwab.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.toblad.khwab.aura.UnityAuraManager
import com.toblad.khwab.ui.theme.KhwabTheme

/**
 * Activity hosting the Aura Settings screen.
 */
class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            KhwabTheme {
                SettingsScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        UnityAuraManager.start()
    }

    override fun onResume() {
        super.onResume()
        UnityAuraManager.resume()
        UnityAuraManager.attachTo(this)
    }

    override fun onPause() {
        UnityAuraManager.detachFrom(this)
        UnityAuraManager.pause()
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        UnityAuraManager.stop()
    }
}
