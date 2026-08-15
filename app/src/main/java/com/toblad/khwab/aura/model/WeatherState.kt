package com.toblad.khwab.aura.model

/**
 * Real-world weather condition sent to Unity Aura.
 *
 * Values map directly to Unity WeatherType names via UnityAuraBridge:
 *   CLEAR  → "Clear"
 *   CLOUDY → "Cloudy"
 *   RAIN   → "Rain"
 *   SNOW   → "Snow"
 *   FOG    → "Fog"
 *   STORM  → "Storm"
 */
enum class WeatherState {
    CLEAR,
    CLOUDY,
    RAIN,
    SNOW,
    FOG,
    STORM
}
