package com.toblad.khwab.executor

import android.content.Context
import android.util.Log
import com.toblad.khwab.aura.AuraBridge
import com.toblad.khwab.aura.model.WeatherState
import com.toblad.khwab.integration.model.execution.ExecutionPlan

/**
 * Executes Aura related commands.
 *
 * All the real work — environment sync, background refresh,
 * ambient sound — is owned centrally by AuraBridge, so this
 * executor just triggers the right AuraBridge call.
 */
class AuraExecutor(
    context: Context
) : AndroidExecutor {

    override fun supports(action: String): Boolean {

        return action.equals("ACTIVATE_AURA", ignoreCase = true) ||
                action.equals("DEACTIVATE_AURA", ignoreCase = true) ||
                action.equals("PREVIEW_AURA_WEATHER", ignoreCase = true)
    }

    override fun execute(plan: ExecutionPlan): Boolean {

        return try {

            when (plan.action.uppercase()) {

                "ACTIVATE_AURA" -> {
                    AuraBridge.activate()
                    Log.d("AuraExecutor", "Aura activated.")
                    true
                }

                "DEACTIVATE_AURA" -> {
                    AuraBridge.deactivate()
                    Log.d("AuraExecutor", "Aura deactivated.")
                    true
                }

                "PREVIEW_AURA_WEATHER" -> {

                    val weather = parseWeather(plan.target ?: "")

                    if (weather == null) {

                        Log.e(
                            "AuraExecutor",
                            "Unrecognized preview weather: '${plan.target}'"
                        )

                        false

                    } else {

                        if (!AuraBridge.isActive()) {
                            AuraBridge.activate()
                        }

                        AuraBridge.updateWeather(weather)

                        Log.d(
                            "AuraExecutor",
                            "Previewing $weather aura. Real weather resumes on the next sync."
                        )

                        true
                    }
                }

                else -> false
            }

        } catch (e: Exception) {
            Log.e("AuraExecutor", e.message ?: "Unknown error")
            false
        }
    }

    private fun parseWeather(raw: String): WeatherState? {

        return when (raw.trim().lowercase()) {
            "clear", "sunny", "sun" -> WeatherState.CLEAR
            "cloudy", "clouds", "overcast" -> WeatherState.CLOUDY
            "rain", "rainy", "rainstorm" -> WeatherState.RAIN
            "snow", "snowy", "snowfall" -> WeatherState.SNOW
            "fog", "foggy", "mist", "misty" -> WeatherState.FOG
            "storm", "stormy", "thunder", "thunderstorm", "lightning" -> WeatherState.STORM
            else -> null
        }
    }
}