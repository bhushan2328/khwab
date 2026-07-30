package com.toblad.khwab.ui.theme

/**
 * Represents the complete active state of the Khwab Theme Engine.
 *
 * Every screen observes this object.
 * Whenever any property changes, Compose automatically updates the UI.
 */
data class ThemeState(

    /**
     * UI mode.
     */
    val mode: ThemeMode,

    /**
     * Current time period.
     */
    val time: TimeTheme,

    /**
     * Current weather.
     */
    val weather: WeatherTheme,

    /**
     * Current season.
     */
    val season: SeasonTheme,

    /**
     * Current visual environment.
     */
    val environment: EnvironmentTheme,

    /**
     * Active color palette.
     */
    val palette: ThemePalette,

    /**
     * Active animation configuration.
     */
    val animation: ThemeAnimation
)