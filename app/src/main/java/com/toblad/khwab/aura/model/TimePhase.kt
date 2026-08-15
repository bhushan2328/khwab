package com.toblad.khwab.aura.model

/**
 * Phase of the day, derived from real solar position or device clock.
 * Used by AuraIconProvider, ambient sound, and Compose UI tint logic.
 */
enum class TimePhase {
    PRE_DAWN,
    SUNRISE,
    MORNING,
    NOON,
    AFTERNOON,
    SUNSET,
    EVENING,
    NIGHT,
    MIDNIGHT
}
