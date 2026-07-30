package com.toblad.khwab.ui.theme

/**
 * Represents the current weather used by the Khwab theme engine.
 *
 * Initially this will be selected manually.
 * Later it can be connected to a live weather provider.
 */
enum class WeatherTheme {

    /**
     * Clear sunny weather.
     */
    CLEAR,

    /**
     * Partly cloudy weather.
     */
    PARTLY_CLOUDY,

    /**
     * Completely cloudy sky.
     */
    CLOUDY,

    /**
     * Light or heavy rain.
     */
    RAIN,

    /**
     * Thunderstorms.
     */
    THUNDER,

    /**
     * Snowfall.
     */
    SNOW,

    /**
     * Fog or mist.
     */
    FOG,

    /**
     * Strong windy conditions.
     */
    WIND
}