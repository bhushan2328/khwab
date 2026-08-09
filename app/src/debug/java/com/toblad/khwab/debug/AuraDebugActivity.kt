package com.toblad.khwab.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.toblad.khwab.ui.theme.KhwabTheme

/**
 * Debug-only Activity that hosts the Aura Testing Console.
 *
 * This file lives in src/debug/ and is therefore compiled only
 * into debug builds. It does not exist in release APKs at all —
 * neither the class nor the manifest entry.
 *
 * Navigate here from HomeScreen when BuildConfig.DEBUG is true.
 */
class AuraDebugActivity : ComponentActivity() {

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
}
