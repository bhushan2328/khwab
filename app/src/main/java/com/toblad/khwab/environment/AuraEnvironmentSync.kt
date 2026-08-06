package com.toblad.khwab.environment

import android.content.Context
import com.toblad.khwab.aura.AuraBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.max

/**
 * Refreshes Aura's environmental inputs — real device
 * location and live weather for that location — and pushes
 * the result into the Aura engine so the active theme
 * reflects actual current conditions.
 *
 * Time is not handled here: Aura always reads the device's
 * real clock internally when generating a theme.
 *
 * Falls back to the last successfully fetched weather when a
 * live fetch is unavailable (no location, no network, etc.),
 * so Aura never silently resets to a default "clear" theme
 * just because the device went offline.
 */
class AuraEnvironmentSync(
    context: Context
) {

    private val locationProvider = LocationProvider(context)

    private val weatherApiClient = WeatherApiClient()

    private val weatherCache = WeatherCache(context)

    /**
     * Loads the last cached weather + location + intensity
     * (if any) into Aura immediately, without touching the
     * network. Call this first on activation so Aura has
     * real conditions to show right away, even before a live
     * sync completes — or when there is no connectivity at
     * all.
     */
    fun hydrateFromCache() {

        val cached = weatherCache.load() ?: return

        AuraBridge.updateLocation(
            cached.latitude,
            cached.longitude
        )

        AuraBridge.updateWeather(cached.weather)

        AuraBridge.updateStormIntensity(cached.stormIntensity)
    }

    /**
     * Fetches the device's current location and the live
     * weather for it, then updates Aura's weather state and
     * storm severity.
     *
     * Safe to call without location permission or network —
     * Aura falls back to its last cached weather in that
     * case, so this never needs to throw.
     */
    suspend fun sync() {

        val location = withTimeoutOrNull(8000) {
            withContext(Dispatchers.IO) {
                locationProvider.getCurrentLocation()
            }
        }

        if (location == null) {
            hydrateFromCache()
            return
        }

        AuraBridge.updateLocation(
            location.latitude,
            location.longitude
        )

        val reading = withContext(Dispatchers.IO) {
            weatherApiClient.fetchWeather(
                location.latitude,
                location.longitude
            )
        }

        if (reading == null) {
            hydrateFromCache()
            return
        }

        val intensity = severityOf(reading)

        weatherCache.save(
            weather = reading.state,
            latitude = location.latitude,
            longitude = location.longitude,
            stormIntensity = intensity
        )

        AuraBridge.updateWeather(reading.state)

        AuraBridge.updateStormIntensity(intensity)
    }

    /**
     * Converts raw wind speed (km/h) and precipitation (mm)
     * into a normalized 0.15–1.0 severity score used to scale
     * how intense rain/lightning visuals appear. Floored at
     * 0.15 so visuals are never fully flattened even in mild
     * conditions.
     */
    private fun severityOf(reading: WeatherApiClient.WeatherReading): Float {

        val windScore = (reading.windSpeedKmh / 80.0).coerceIn(0.0, 1.0)
        val rainScore = (reading.precipitationMm / 15.0).coerceIn(0.0, 1.0)

        return max(windScore, rainScore).coerceIn(0.15, 1.0).toFloat()
    }
}