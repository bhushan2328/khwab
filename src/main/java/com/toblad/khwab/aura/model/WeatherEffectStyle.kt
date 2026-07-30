package com.toblad.khwab.aura.model

/**
 * Represents atmospheric visual effects
 * rendered by Aura.
 *
 * These effects are independent of the
 * sky and celestial bodies, allowing
 * multiple visual layers to be combined.
 */
enum class WeatherEffectStyle {

    /**
     * No weather effect.
     */
    NONE,

    /**
     * Light rain.
     */
    LIGHT_RAIN,

    /**
     * Heavy rain.
     */
    HEAVY_RAIN,

    /**
     * Thunderstorm with lightning.
     */
    THUNDERSTORM,

    /**
     * Light snowfall.
     */
    LIGHT_SNOW,

    /**
     * Heavy snowfall.
     */
    HEAVY_SNOW,

    /**
     * Mist or haze.
     */
    MIST,

    /**
     * Dense fog.
     */
    FOG,

    /**
     * Dust or sand particles.
     */
    DUST,

    /**
     * Strong wind effects.
     */
    WIND,

    /**
     * Lightning flashes only.
     */
    LIGHTNING
}
