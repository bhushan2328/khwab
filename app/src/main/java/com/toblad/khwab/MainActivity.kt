package com.toblad.khwab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.toblad.khwab.ui.theme.HomeScreen
import com.toblad.khwab.ui.theme.KhwabTheme
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.toblad.khwab.permission.PermissionManager
import com.toblad.khwab.state.AssistantState
import com.toblad.khwab.state.AssistantStateManager
import android.content.Intent
import android.os.Build
import com.toblad.khwab.service.VoiceService




class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

                } else {

                    AssistantStateManager.updateState(
                        AssistantState.ERROR
                    )
                }
            }

        enableEdgeToEdge()

        setContent {

            KhwabTheme {

                HomeScreen(

                    onStartClick = {

                        permissionLauncher.launch(
                            permissionManager.requiredPermissions()
                        )

                    },

                    onStopClick = {

                        val serviceIntent = Intent(this, VoiceService::class.java)

                        stopService(serviceIntent)

                        AssistantStateManager.updateState(
                            AssistantState.STOPPED
                        )


                    }

                )

            }

        }

    }

}