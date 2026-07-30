package com.toblad.khwab.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Global Theme Manager.
 *
 * Holds the active ThemeState and provides methods
 * to update individual theme properties.
 */
object ThemeManager {

    var themeState by mutableStateOf(
        ThemeEngine.createTheme()
    )
        private set

    /**
     * Rebuilds the current theme.
     */
    private fun rebuild(

        mode: ThemeMode = themeState.mode,

        time: TimeTheme = themeState.time,

        weather: WeatherTheme = themeState.weather,

        season: SeasonTheme = themeState.season,

        environment: EnvironmentTheme = themeState.environment

    ) {

        themeState = ThemeEngine.createTheme(
            mode = mode,
            time = time,
            weather = weather,
            season = season,
            environment = environment
        )
    }

    fun setThemeMode(mode: ThemeMode) =
        rebuild(mode = mode)

    fun setTimeTheme(time: TimeTheme) =
        rebuild(time = time)

    fun setWeatherTheme(weather: WeatherTheme) =
        rebuild(weather = weather)

    fun setSeasonTheme(season: SeasonTheme) =
        rebuild(season = season)

    fun setEnvironmentTheme(environment: EnvironmentTheme) =
        rebuild(environment = environment)

    /**
     * Refreshes themes that depend on the current date and time.
     */
    fun refresh() {

        rebuild(
            time = TimeTheme.current(),
            season = SeasonTheme.current()
        )
    }

    /**
     * Restores the default Khwab appearance.
     */
    fun reset() {

        themeState = ThemeEngine.createTheme()
    }
}