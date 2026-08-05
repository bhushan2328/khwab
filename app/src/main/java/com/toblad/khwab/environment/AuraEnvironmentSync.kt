package com.toblad.khwab.environment

import android.content.Context
import com.toblad.khwab.aura.AuraBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Refreshes Aura's environmental inputs — real device
 * location and live weather for that location — and pushes
 * the result into the Aura engine so the active theme
 * reflects actual current conditions.
 *
 * Time is not handled here: Aura always reads the device's
 * real clock internally when generating a theme.
 */
class AuraEnvironmentSync(
    context: Context
) {

    private val locationProvider = LocationProvider(context)

    private val weatherApiClient = WeatherApiClient()

    /**
     * Fetches the device's current location and the live
     * weather for it, then updates Aura's weather state.
     *
     * Safe to call without location permission or network —
     * Aura simply keeps its last known / default weather in
     * that case, so this never needs to throw.
     */
    suspend fun sync() {

        val location = withTimeoutOrNull(8000) {
            withContext(Dispatchers.IO) {
                locationProvider.getCurrentLocation()
            }
        } ?: return

        val weather = withContext(Dispatchers.IO) {
            weatherApiClient.fetchWeather(
                location.latitude,
                location.longitude
            )
        } ?: return

        AuraBridge.updateWeather(weather)
    }
}