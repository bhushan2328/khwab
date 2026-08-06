package com.toblad.khwab.environment

import com.toblad.khwab.aura.model.WeatherState
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Minimal client for the Open-Meteo current-weather API
 * (https://open-meteo.com). No API key is required.
 *
 * Must be called from a background thread — it performs a
 * blocking network request.
 */
class WeatherApiClient {

    /**
     * A single live weather reading: the mapped Aura state
     * plus the raw wind speed / precipitation used to gauge
     * real-world severity.
     */
    data class WeatherReading(
        val state: WeatherState,
        val windSpeedKmh: Double,
        val precipitationMm: Double
    )

    /**
     * Fetches the current weather for the given coordinates
     * and maps it to an Aura [WeatherState], along with wind
     * speed and precipitation for severity scoring.
     *
     * Returns null if the request fails for any reason (no
     * network, timeout, malformed response, etc).
     */
    fun fetchWeather(latitude: Double, longitude: Double): WeatherReading? {

        return try {

            val url = URL(
                String.format(
                    Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=weather_code,wind_speed_10m,precipitation&timezone=auto",
                    latitude,
                    longitude
                )
            )

            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            val responseCode = connection.responseCode

            if (responseCode != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                return null
            }

            val body = connection.inputStream
                .bufferedReader()
                .use { it.readText() }

            connection.disconnect()

            val current = JSONObject(body).getJSONObject("current")

            val weatherCode = current.getInt("weather_code")
            val windSpeed = current.optDouble("wind_speed_10m", 0.0)
            val precipitation = current.optDouble("precipitation", 0.0)

            WeatherReading(
                state = mapWeatherCode(weatherCode),
                windSpeedKmh = windSpeed,
                precipitationMm = precipitation
            )

        } catch (e: Exception) {
            null
        }
    }

    /**
     * Maps a WMO weather code (as returned by Open-Meteo) to
     * an Aura [WeatherState].
     */
    private fun mapWeatherCode(code: Int): WeatherState {

        return when (code) {

            0, 1 -> WeatherState.CLEAR

            2, 3 -> WeatherState.CLOUDY

            45, 48 -> WeatherState.FOG

            51, 53, 55, 56, 57,
            61, 63, 65, 66, 67,
            80, 81, 82 -> WeatherState.RAIN

            71, 73, 75, 77,
            85, 86 -> WeatherState.SNOW

            95, 96, 99 -> WeatherState.STORM

            else -> WeatherState.CLEAR
        }
    }
}