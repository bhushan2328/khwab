package com.toblad.khwab.aura.model

/**
 * Represents the overall lighting used
 * throughout the Aura scene.
 *
 * This controls brightness, warmth and
 * illumination independent of the sky.
 */
enum class AmbientLightStyle {

    /**
     * Very low light before sunrise.
     */
    PRE_DAWN,

    /**
     * Warm sunrise lighting.
     */
    SUNRISE,

    /**
     * Bright morning light.
     */
    MORNING,

    /**
     * Strong midday sunlight.
     */
    NOON,

    /**
     * Soft afternoon lighting.
     */
    AFTERNOON,

    /**
     * Warm golden sunset.
     */
    SUNSET,

    /**
     * Dim evening light.
     */
    EVENING,

    /**
     * Natural moonlight.
     */
    MOONLIGHT,

    /**
     * Very dark night.
     */
    NIGHT,

    /**
     * Diffused lighting caused by clouds.
     */
    OVERCAST,

    /**
     * Low-visibility lighting due to fog.
     */
    FOG
}
