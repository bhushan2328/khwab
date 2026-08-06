package com.toblad.khwab.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
}
