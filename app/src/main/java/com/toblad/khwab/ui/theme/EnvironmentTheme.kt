package com.toblad.khwab.ui.theme

/**
 * Represents the visual environment of Khwab.
 *
 * The environment is independent of:
 * - Time
 * - Weather
 * - Season
 *
 * Example:
 * FOREST + NIGHT + RAIN
 * BEACH + SUNSET + CLEAR
 * SPACE + MORNING + CLOUDY
 */
enum class EnvironmentTheme {

    /**
     * Uses the standard Khwab appearance.
     */
    DEFAULT,

    /**
     * Snow-covered mountains and valleys.
     */
    MOUNTAINS,

    /**
     * Dense green forest.
     */
    FOREST,

    /**
     * Ocean and beach.
     */
    BEACH,

    /**
     * Modern city skyline.
     */
    CITY,

    /**
     * Lakes and rivers.
     */
    LAKE,

    /**
     * Desert landscape.
     */
    DESERT,

    /**
     * Cherry blossom garden.
     */
    CHERRY_BLOSSOM,

    /**
     * Traditional Japanese garden.
     */
    JAPANESE_GARDEN,

    /**
     * Cozy snow cabin.
     */
    SNOW_CABIN,

    /**
     * Rainy window atmosphere.
     */
    RAIN_WINDOW,

    /**
     * Deep space environment.
     */
    SPACE
}