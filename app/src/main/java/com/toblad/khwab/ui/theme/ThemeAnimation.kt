package com.toblad.khwab.ui.theme

/**
 * Defines animation characteristics for the current theme.
 *
 * All values are expressed in milliseconds or normalized factors.
 */
data class ThemeAnimation(

    /* General */

    val transitionDuration: Int,
    val fadeDuration: Int,
    val scaleDuration: Int,

    /* Background */

    val backgroundAnimationSpeed: Float,

    /* Glow */

    val glowEnabled: Boolean,
    val glowIntensity: Float,

    /* Pulse */

    val pulseEnabled: Boolean,
    val pulseDuration: Int,

    /* Floating */

    val floatingEnabled: Boolean,
    val floatingSpeed: Float,

    /* Weather */

    val weatherAnimationEnabled: Boolean,
    val weatherIntensity: Float,

    /* Particles */

    val particleEnabled: Boolean,
    val particleDensity: Float,

    /* Blur */

    val blurEnabled: Boolean,
    val blurRadius: Float,

    /* Performance */

    val animationEnabled: Boolean
)