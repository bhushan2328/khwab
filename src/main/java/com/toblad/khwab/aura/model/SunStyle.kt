package com.toblad.khwab.aura.model

/**
 * Represents how the sun should appear.
 *
 * ThemeEngine selects one of these styles
 * based on the current time and weather.
 */
enum class SunStyle {

    /**
     * Sun is below the horizon.
     */
    HIDDEN,

    /**
     * Sun is rising.
     */
    RISING,

    /**
     * Bright morning sun.
     */
    MORNING,

    /**
     * High noon sun.
     */
    NOON,

    /**
     * Warm afternoon sun.
     */
    AFTERNOON,

    /**
     * Sun is setting.
     */
    SETTING,

    /**
     * Sun is partially obscured by clouds.
     */
    BEHIND_CLOUDS,

    /**
     * Sun is barely visible through fog.
     */
    FOGGY
}
