package com.toblad.khwab.executor

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import com.toblad.khwab.integration.model.execution.ExecutionPlan

/**
 * Handles CHANGE_SETTING actions.
 *
 * [ExecutionPlan.target] — the setting to open. Recognised values (case-insensitive):
 *   "wifi"        → Wi-Fi settings
 *   "bluetooth"   → Bluetooth settings
 *   "accessibility" → Accessibility settings
 *   "display"     → Display settings
 *   "sound"       → Sound settings
 *   "battery"     → Battery settings
 *   "location"    → Location settings
 *   anything else → General settings screen
 */
class SettingExecutor(private val context: Context) : AndroidExecutor {

    override fun supports(action: String): Boolean =
        action.equals("CHANGE_SETTING", ignoreCase = true)

    override fun execute(plan: ExecutionPlan): Boolean {
        val target = plan.target?.trim()?.lowercase() ?: ""
        val action = when {
            target.contains("wifi") || target.contains("wi-fi") ->
                Settings.ACTION_WIFI_SETTINGS
            target.contains("bluetooth") ->
                Settings.ACTION_BLUETOOTH_SETTINGS
            target.contains("accessibility") ->
                Settings.ACTION_ACCESSIBILITY_SETTINGS
            target.contains("display") || target.contains("brightness") ->
                Settings.ACTION_DISPLAY_SETTINGS
            target.contains("sound") || target.contains("volume") ->
                Settings.ACTION_SOUND_SETTINGS
            target.contains("battery") ->
                Settings.ACTION_BATTERY_SAVER_SETTINGS
            target.contains("location") || target.contains("gps") ->
                Settings.ACTION_LOCATION_SOURCE_SETTINGS
            else ->
                Settings.ACTION_SETTINGS
        }
        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.d("SettingExecutor", "Opened settings: $action")
            true
        } catch (e: Exception) {
            Log.e("SettingExecutor", "Failed to open settings: ${e.message}")
            false
        }
    }
}
