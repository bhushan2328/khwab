package com.toblad.khwab

import android.Manifest
import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.toblad.khwab.aura.AuraBridge
import com.toblad.khwab.chat.ChatActivity
import com.toblad.khwab.logging.LogModule
import com.toblad.khwab.logging.Logger
import com.toblad.khwab.permission.AccessibilityPermissionHelper
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

    // Set to true when we navigate to Accessibility Settings so the user can
    // enable KhwabAccessibilityService. onResume dismisses the dialog automatically
    // if they have enabled it.
    private var pendingAccessibilityPermission = false

    // Compose state — drives the accessibility onboarding dialog.
    private var showAccessibilityDialog by mutableStateOf(false)

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

        // If the user returned from Accessibility Settings, re-check and dismiss
        // the onboarding dialog if they have now enabled the service.
        if (pendingAccessibilityPermission) {
            pendingAccessibilityPermission = false
            if (AccessibilityPermissionHelper.isEnabledBySystem(this)) {
                showAccessibilityDialog = false
            }
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

        // fix #1: declared before the launcher so the lambda can capture it
        var permissionDialogState by mutableStateOf<PermissionDialogState?>(null)

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
                    val denied = permissions.filterValues { !it }.keys
                    Logger.error(LogModule.SYSTEM, "Required permissions denied: $denied")
                    showPermissionDeniedDialog(denied) { permissionDialogState = it }
                }
            }

        setContent {

            KhwabTheme {

                // fix #1: Compose AlertDialog uses KhwabTheme automatically
                permissionDialogState?.let { state ->
                    AlertDialog(
                        onDismissRequest = { permissionDialogState = null },
                        title = { Text("Permissions Required") },
                        text  = { Text(state.message) },
                        confirmButton = {
                            TextButton(onClick = {
                                permissionDialogState = null
                                if (state.isPermanentlyDenied) {
                                    startActivity(
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", packageName, null)
                                        }
                                    )
                                } else {
                                    startAssistant()
                                }
                            }) {
                                Text(if (state.isPermanentlyDenied) "Open Settings" else "Grant Permissions")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { permissionDialogState = null }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                // Accessibility onboarding dialog — shown after assistant starts
                // if KhwabAccessibilityService is not yet enabled.
                if (showAccessibilityDialog) {
                    AlertDialog(
                        onDismissRequest = { showAccessibilityDialog = false },
                        title = { Text("Enable Accessibility Access") },
                        text  = {
                            Text(
                                "To control other apps with your voice, Khwab needs " +
                                "Accessibility access.\n\nGo to:\n" +
                                "Settings → Accessibility → Downloaded Apps → Khwab\n\n" +
                                "Toggle it on, then return here."
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                pendingAccessibilityPermission = true
                                AccessibilityPermissionHelper.openAccessibilitySettings(
                                    this@MainActivity
                                )
                            }) {
                                Text("Open Settings")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAccessibilityDialog = false }) {
                                Text("Skip for now")
                            }
                        }
                    )
                }

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
                            permissionLauncher.launch(permissionManager.requiredPermissions())
                        },
                        onStopClick = { stopAssistant() },
                        onChatClick = {
                            // fix #7: modern ActivityOptions replaces deprecated overridePendingTransition
                            val opts = ActivityOptions.makeCustomAnimation(
                                this@MainActivity,
                                android.R.anim.fade_in,
                                android.R.anim.fade_out
                            ).toBundle()
                            startActivity(Intent(this@MainActivity, ChatActivity::class.java), opts)
                        },
                        onSettingsClick = {
                            val opts = ActivityOptions.makeCustomAnimation(
                                this@MainActivity,
                                android.R.anim.fade_in,
                                android.R.anim.fade_out
                            ).toBundle()
                            startActivity(Intent(this@MainActivity, SettingsActivity::class.java), opts)
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

        // Prompt the user to enable Accessibility if they haven't already.
        // This is non-blocking — the assistant runs with or without it.
        if (!AccessibilityPermissionHelper.isEnabledBySystem(this)) {
            Logger.info(LogModule.ACCESSIBILITY, "Accessibility service not enabled — showing onboarding dialog")
            showAccessibilityDialog = true
        }
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

    // fix #1: data class holding dialog content — shown as Compose AlertDialog in setContent
    private data class PermissionDialogState(
        val message: String,
        val isPermanentlyDenied: Boolean
    )

    private fun showPermissionDeniedDialog(
        denied: Set<String>,
        dialogStateSetter: (PermissionDialogState?) -> Unit
    ) {
        AssistantStateManager.updateState(AssistantState.ERROR)

        val deniedLabels = denied.joinToString("\n") { permission ->
            when (permission) {
                Manifest.permission.RECORD_AUDIO           -> "• Microphone (required to hear your voice)"
                Manifest.permission.ACCESS_COARSE_LOCATION -> "• Location (used for weather-aware themes)"
                Manifest.permission.POST_NOTIFICATIONS     -> "• Notifications (used for the assistant status bar)"
                else -> "• ${permission.substringAfterLast('.')}"
            }
        }

        val isPermanentlyDenied = denied.any { !shouldShowRequestPermissionRationale(it) }

        val message = if (isPermanentlyDenied) {
            "The following permissions were permanently denied:\n\n$deniedLabels\n\nPlease open App Settings and grant them manually."
        } else {
            "Khwab needs the following permissions to work:\n\n$deniedLabels"
        }

        dialogStateSetter(PermissionDialogState(message, isPermanentlyDenied))
    }
}