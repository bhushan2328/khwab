package com.toblad.khwab.ui.theme

/**
 * Controls how dynamic the Khwab UI should be.
 */
enum class ThemeMode {

    /**
     * Static colors with minimal animation.
     * Best for battery saving.
     */
    MINIMAL,

    /**
     * Adaptive colors with subtle animations.
     * Recommended default.
     */
    BALANCED,

    /**
     * Full adaptive experience:
     * - Dynamic backgrounds
     * - Weather effects
     * - Rich animations
     */
    IMMERSIVE
}