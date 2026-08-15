package com.toblad.khwab.aura.model

/**
 * User-facing configuration for Khwab Aura.
 *
 * Persisted by AuraConfigStore across app restarts.
 * Live/transient fields (latitude, longitude, stormIntensity) are
 * not persisted — they are re-fetched by AuraEnvironmentSync on each activation.
 */
data class AuraConfig(

    /** Master on/off switch for Aura. */
    val enabled: Boolean = false,

    /** Automatically update Unity sky based on the real device clock. */
    val autoTime: Boolean = true,

    /** Automatically update Unity weather from live location weather. */
    val autoWeather: Boolean = true,

    /** Enables background animations (passed to Unity as a hint). */
    val animationsEnabled: Boolean = true,

    /** Enables ambient sound playback on the Android side. */
    val ambientSoundEnabled: Boolean = true,

    /** How often to re-fetch weather, in minutes. */
    val refreshIntervalMinutes: Int = 15,

    /** Device latitude — null until supplied by LocationProvider. */
    val latitude: Double? = null,

    /** Device longitude — null until supplied by LocationProvider. */
    val longitude: Double? = null,

    /** Storm severity 0.0–1.0, derived from wind/precipitation. */
    val stormIntensity: Float = 0.5f
)
