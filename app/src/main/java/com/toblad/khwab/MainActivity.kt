package com.toblad.khwab

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import android.app.AlertDialog
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

    // Set to true when we send the user to the overlay permission Settings screen.
    // onResume checks this flag to call startAssistant() once they return.
    private var pendingOverlayPermission = false

    override fun onResume() {
        super.onResume()
        if (pendingOverlayPermission) {
            pendingOverlayPermission = false
            val permissionManager = PermissionManager(this)
            if (permissionManager.hasOverlayPermission()) {
                startAssistant()
            }
            // If still not granted, leave the user on the home screen to try again.
        }
    }

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
                        pendingOverlayPermission = true
                        permissionManager.requestOverlayPermission()
                    } else {
                        startAssistant()
                    }

                } else {

                    val denied = permissions
                        .filterValues { !it }
                        .keys

                    Logger.error(
                        LogModule.SYSTEM,
                        "Required permissions denied: $denied"
                    )

                    showPermissionDeniedDialog(denied)
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
                                            @Suppress("DEPRECATION")
                                            overridePendingTransition(
                                                android.R.anim.fade_in,
                                                android.R.anim.fade_out
                                            )
                
                                        },
                
                                        onSettingsClick = {
                
                                            startActivity(
                                                Intent(
                                                    this@MainActivity,
                                                    SettingsActivity::class.java
                                                )
                                            )
                                            @Suppress("DEPRECATION")
                                            overridePendingTransition(
                                                android.R.anim.fade_in,
                                                android.R.anim.fade_out
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

    private fun showPermissionDeniedDialog(denied: Set<String>) {
        AssistantStateManager.updateState(AssistantState.ERROR)

        val deniedLabels = denied.joinToString("\n") { permission ->
            when (permission) {
                Manifest.permission.RECORD_AUDIO       -> "• Microphone (required to hear your voice)"
                Manifest.permission.ACCESS_COARSE_LOCATION -> "• Location (used for weather-aware themes)"
                Manifest.permission.POST_NOTIFICATIONS -> "• Notifications (used for the assistant status bar)"
                else -> "• ${permission.substringAfterLast('.')}"
            }
        }

        val isPermanentlyDenied = denied.any { permission ->
            !shouldShowRequestPermissionRationale(permission)
        }

        val message = if (isPermanentlyDenied) {
            "The following permissions were permanently denied:\n\n$deniedLabels\n\n" +
            "Please open App Settings and grant them manually."
        } else {
            "Khwab needs the following permissions to work:\n\n$deniedLabels"
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage(message)
            .setNegativeButton("Cancel") { _, _ -> /* stay on ERROR state */ }

        if (isPermanentlyDenied) {
            dialog.setPositiveButton("Open Settings") { _, _ ->
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                )
            }
        } else {
            dialog.setPositiveButton("Grant Permissions") { _, _ ->
                startAssistant()
            }
        }

        dialog.show()
    }
}