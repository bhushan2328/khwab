package com.toblad.khwab.aura

import android.content.Context
import com.toblad.khwab.aura.model.AuraConfig

/**
 * Persists the user-facing Aura preferences (not live/transient
 * fields like location or storm intensity, which are re-fetched
 * independently) so they survive an app restart.
 */
class AuraConfigStore(
    context: Context
) {

    private val prefs = context.applicationContext
        .getSharedPreferences("aura_user_settings", Context.MODE_PRIVATE)

    fun save(config: AuraConfig) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, config.enabled)
            .putBoolean(KEY_AUTO_TIME, config.autoTime)
            .putBoolean(KEY_AUTO_WEATHER, config.autoWeather)
            .putBoolean(KEY_ANIMATIONS, config.animationsEnabled)
            .putBoolean(KEY_AMBIENT_SOUND, config.ambientSoundEnabled)
            .putInt(KEY_REFRESH_INTERVAL, config.refreshIntervalMinutes)
            .apply()
    }

    /**
     * Returns [base] with any saved user preferences applied
     * on top. Live/transient fields (latitude, longitude,
     * stormIntensity) are always taken from [base] untouched.
     * If nothing has been saved yet, returns [base] unchanged.
     */
    fun applySaved(base: AuraConfig): AuraConfig {

        if (!prefs.contains(KEY_ENABLED)) return base

        return base.copy(
            enabled = prefs.getBoolean(KEY_ENABLED, base.enabled),
            autoTime = prefs.getBoolean(KEY_AUTO_TIME, base.autoTime),
            autoWeather = prefs.getBoolean(KEY_AUTO_WEATHER, base.autoWeather),
            animationsEnabled = prefs.getBoolean(KEY_ANIMATIONS, base.animationsEnabled),
            ambientSoundEnabled = prefs.getBoolean(KEY_AMBIENT_SOUND, base.ambientSoundEnabled),
            refreshIntervalMinutes = prefs.getInt(KEY_REFRESH_INTERVAL, base.refreshIntervalMinutes)
        )
    }

    companion object {
        private const val KEY_ENABLED = "enabled"
        private const val KEY_AUTO_TIME = "auto_time"
        private const val KEY_AUTO_WEATHER = "auto_weather"
        private const val KEY_ANIMATIONS = "animations_enabled"
        private const val KEY_AMBIENT_SOUND = "ambient_sound_enabled"
        private const val KEY_REFRESH_INTERVAL = "refresh_interval_minutes"
    }
}