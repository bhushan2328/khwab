package com.toblad.khwab.aura.model

/**
 * Complete visual profile used by AuraRenderer.
 *
 * ThemeEngine produces this object and the
 * renderer consumes it without making any
 * visual decisions of its own.
 */
data class ThemeProfile(

    /**
     * Sky appearance.
     */
    val sky: SkyStyle,

    /**
     * Cloud appearance.
     */
    val clouds: CloudStyle,

    /**
     * Sun appearance.
     */
    val sun: SunStyle,

    /**
     * Moon appearance.
     */
    val moon: MoonStyle,

    /**
     * Atmospheric weather effects.
     */
    val weatherEffect: WeatherEffectStyle,

    /**
     * Scene lighting.
     */
    val ambientLight: AmbientLightStyle,

    /**
     * Scene animation behaviour.
     */
    val animation: AnimationStyle
)
