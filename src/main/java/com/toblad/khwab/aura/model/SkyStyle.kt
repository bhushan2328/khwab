package com.toblad.khwab.aura.model

/**
 * Represents the visual appearance of the sky.
 *
 * ThemeEngine selects one of these styles based
 * on the current time and weather.
 */
enum class SkyStyle {

    /**
     * Dark blue before sunrise.
     */
    PRE_DAWN,

    /**
     * Orange and pink sunrise.
     */
    SUNRISE,

    /**
     * Bright morning sky.
     */
    MORNING,

    /**
     * Bright midday sky.
     */
    NOON,

    /**
     * Soft afternoon blue.
     */
    AFTERNOON,

    /**
     * Golden sunset.
     */
    SUNSET,

    /**
     * Purple evening sky.
     */
    EVENING,

    /**
     * Night sky with stars.
     */
    NIGHT,

    /**
     * Very dark midnight sky.
     */
    MIDNIGHT,

    /**
     * Overcast cloudy sky.
     */
    CLOUDY,

    /**
     * Storm clouds.
     */
    STORM,

    /**
     * Fog-covered sky.
     */
    FOG
}
