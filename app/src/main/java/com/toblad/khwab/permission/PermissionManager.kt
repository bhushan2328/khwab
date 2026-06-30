package com.toblad.khwab.permission

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher

class PermissionManager(
    private val activity: Activity
) {

    fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(activity)
    }

    fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${activity.packageName}")
        )
        activity.startActivity(intent)
    }

    fun requiredPermissions(): Array<String> {

        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        return permissions.toTypedArray()
    }

    fun requestRuntimePermissions(
        launcher: ActivityResultLauncher<Array<String>>
    ) {
        launcher.launch(requiredPermissions())
    }
}