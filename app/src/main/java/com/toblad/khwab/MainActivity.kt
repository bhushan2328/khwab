package com.toblad.khwab

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.toblad.khwab.aura.AuraBridge
import com.toblad.khwab.chat.ChatActivity
import com.toblad.khwab.logging.LogModule
import com.toblad.khwab.logging.Logger
import com.toblad.khwab.permission.PermissionManager
import com.toblad.khwab.service.VoiceService
import com.toblad.khwab.settings.SettingsActivity
import com.toblad.khwab.speech.ModelDownloadManager
import com.toblad.khwab.speech.ModelDownloadScreen
import com.toblad.khwab.state.AssistantState
import com.toblad.khwab.state.AssistantStateManager
import com.toblad.khwab.ui.theme.HomeScreen
import com.toblad.khwab.ui.theme.KhwabTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Logger.initialize(applicationContext)

        Logger.info(
            LogModule.SYSTEM,
            "Khwab application started"
        )

        // Restore any saved Aura preferences (enabled state,
        // follow-time, follow-weather, animations, ambient
        // sound, refresh interval) before Aura is used anywhere.
        AuraBridge.initialize(applicationContext)

        enableEdgeToEdge()

        val permissionManager = PermissionManager(this)

        val permissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->

                val allGranted = permissions.values.all { it }

                if (allGranted) {

                    if (!permissionManager.hasOverlayPermission()) {
                        permissionManager.requestOverlayPermission()
                    } else {
                        startAssistant()
                    }

                } else {

                    Logger.error(
                        LogModule.SYSTEM,
                        "Required permissions denied"
                    )

                    AssistantStateManager.updateState(
                        AssistantState.ERROR
                    )
                }
            }

        setContent {

            KhwabTheme {

                // On first launch the Whisper model files are not yet on disk.
                // Show the download screen until they are ready, then show Home.
                var modelsReady by rememberSaveable {
                    mutableStateOf(ModelDownloadManager.modelsReady(applicationContext))
                }

                if (!modelsReady) {
                    ModelDownloadScreen(
                        onReady = { modelsReady = true }
                    )
                } else {
                    HomeScreen(

                        onStartClick = {

                            permissionLauncher.launch(
                                permissionManager.requiredPermissions()
                            )

                        },

                        onStopClick = {

                            stopAssistant()

                        },

                        onChatClick = {

                            startActivity(
                                Intent(
                                    this@MainActivity,
                                    ChatActivity::class.java
                                )
                            )

                        },

                        onSettingsClick = {

                            startActivity(
                                Intent(
                                    this@MainActivity,
                                    SettingsActivity::class.java
                                )
                            )

                        }

                    )
                }

            }

        }
    }

    private fun startAssistant() {

        Logger.info(
            LogModule.SYSTEM,
            "Starting VoiceService"
        )

        val serviceIntent = Intent(this, VoiceService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        AssistantStateManager.updateState(
            AssistantState.RUNNING
        )
    }

    private fun stopAssistant() {

        Logger.info(
            LogModule.SYSTEM,
            "Stopping VoiceService"
        )

        val serviceIntent = Intent(this, VoiceService::class.java)

        stopService(serviceIntent)

        AssistantStateManager.updateState(
            AssistantState.STOPPED
        )
    }
}