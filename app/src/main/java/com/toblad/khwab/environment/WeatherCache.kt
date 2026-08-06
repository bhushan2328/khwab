package com.toblad.khwab.environment

import android.content.Context
import com.toblad.khwab.aura.model.WeatherState

/**
 * Persists the last successfully fetched weather, location
 * and storm intensity so Aura has something real to show
 * immediately on cold start, and can fall back to it whenever
 * a live fetch fails (e.g. no internet connection).
 */
class WeatherCache(
    context: Context
) {

    private val prefs = context.applicationContext
        .getSharedPreferences("aura_weather_cache", Context.MODE_PRIVATE)

    fun save(
        weather: WeatherState,
        latitude: Double,
        longitude: Double,
        stormIntensity: Float
    ) {
        prefs.edit()
            .putString(KEY_WEATHER, weather.name)
            .putFloat(KEY_LAT, latitude.toFloat())
            .putFloat(KEY_LON, longitude.toFloat())
            .putFloat(KEY_INTENSITY, stormIntensity)
            .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    data class CachedEntry(
        val weather: WeatherState,
        val latitude: Double,
        val longitude: Double,
        val stormIntensity: Float,
        val fetchedAtMillis: Long
    )

    fun load(): CachedEntry? {

        val weatherName = prefs.getString(KEY_WEATHER, null) ?: return null

        val weather = try {
            WeatherState.valueOf(weatherName)
        } catch (e: IllegalArgumentException) {
            return null
        }

        if (!prefs.contains(KEY_LAT) || !prefs.contains(KEY_LON)) return null

        return CachedEntry(
            weather = weather,
            latitude = prefs.getFloat(KEY_LAT, 0f).toDouble(),
            longitude = prefs.getFloat(KEY_LON, 0f).toDouble(),
            stormIntensity = prefs.getFloat(KEY_INTENSITY, 0.5f),
            fetchedAtMillis = prefs.getLong(KEY_TIMESTAMP, 0L)
        )
    }

    companion object {
        private const val KEY_WEATHER = "weather"
        private const val KEY_LAT = "lat"
        private const val KEY_LON = "lon"
        private const val KEY_INTENSITY = "intensity"
        private const val KEY_TIMESTAMP = "timestamp"
    }
}