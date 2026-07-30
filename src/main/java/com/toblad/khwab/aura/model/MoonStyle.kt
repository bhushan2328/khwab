package com.toblad.khwab.aura.model

/**
 * Represents how the moon should appear.
 *
 * ThemeEngine selects one of these styles
 * based on the current time and future
 * lunar calculations.
 */
enum class MoonStyle {

    /**
     * Moon is below the horizon.
     */
    HIDDEN,

    /**
     * Thin waxing crescent.
     */
    CRESCENT,

    /**
     * Half moon.
     */
    HALF,

    /**
     * Bright full moon.
     */
    FULL,

    /**
     * Waning moon.
     */
    WANING,

    /**
     * Moon is visible through clouds.
     */
    BEHIND_CLOUDS,

    /**
     * Moon is faint due to fog.
     */
    FOGGY
}
