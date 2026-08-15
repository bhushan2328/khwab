package com.toblad.khwab.aura.model

/**
 * Lightweight snapshot of the current Unity Aura state, used by
 * Android-side Compose UI and the debug console.
 *
 * This replaces the old 2D AuraTheme from the :aura module.
 * All rendering intelligence lives in Unity; this data class
 * carries only what the Android UI layer needs to observe.
 */
data class AuraTheme(
    /** Current Aura lifecycle state. */
    val auraState: AuraState = AuraState.OFF,

    /** Current time-of-day phase — used for icon selection and UI tints. */
    val timePhase: TimePhase = TimePhase.MORNING,

    /** Current weather condition — used for icon selection and ambient sound. */
    val weatherState: WeatherState = WeatherState.CLEAR,

    /** Whether Aura is enabled (mirrors AuraConfig.enabled). */
    val enabled: Boolean = false,

    /** Whether background animations are enabled. */
    val animationsEnabled: Boolean = true,

    /**
     * Normalised solar elevation for approximate Compose UI tinting.
     * +1 = solar noon, 0 = horizon, -1 = midnight.
     * Derived from the device clock; not from Unity.
     */
    val solarElevNorm: Float = 0.5f,

    /** Approximate lunar illumination 0–1. Used for icon/color hints. */
    val moonIlluminationFraction: Float = 0.0f,

    /** True if time phase was derived from GPS location. */
    val isSolarAccurate: Boolean = false
)
