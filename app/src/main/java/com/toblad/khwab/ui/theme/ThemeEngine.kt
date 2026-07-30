package com.toblad.khwab.ui.theme

import androidx.compose.ui.graphics.Brush

/**
 * Generates the active ThemeState for the Khwab UI.
 *
 * This is the central brain of the theme system.
 */
object ThemeEngine {

    /**
     * Creates a complete ThemeState from the current inputs.
     */
    fun createTheme(

        mode: ThemeMode = ThemeMode.BALANCED,

        time: TimeTheme = TimeTheme.current(),

        weather: WeatherTheme = WeatherTheme.CLEAR,

        season: SeasonTheme = SeasonTheme.current(),

        environment: EnvironmentTheme = EnvironmentTheme.DEFAULT

    ): ThemeState {

        val palette = createPalette(
            time = time,
            weather = weather,
            season = season,
            environment = environment
        )

        val animation = createAnimation(mode)

        return ThemeState(
            mode = mode,
            time = time,
            weather = weather,
            season = season,
            environment = environment,
            palette = palette,
            animation = animation
        )
    }

    /**
     * Creates the color palette.
     *
     * V1 uses the existing Khwab colors.
     * Future versions will generate palettes dynamically.
     */
    private fun createPalette(
        time: TimeTheme,
        weather: WeatherTheme,
        season: SeasonTheme,
        environment: EnvironmentTheme
    ): ThemePalette {

        return ThemePalette(

            background = KhwabBackground,

            backgroundGradient = Brush.verticalGradient(
                listOf(
                    KhwabBackground,
                    KhwabCard
                )
            ),

            surface = KhwabCard,
            card = KhwabCard,
            border = KhwabGray,
            shadow = KhwabBlack,

            primary = KhwabBlue,
            secondary = KhwabGreen,
            accent = KhwabYellow,

            textPrimary = KhwabWhite,
            textSecondary = KhwabGray,
            textHint = KhwabGrayDark,

            success = KhwabGreen,
            warning = KhwabYellow,
            error = KhwabRed,
            info = KhwabBlue,

            glow = KhwabListening,
            overlay = KhwabBlack.copy(alpha = 0.20f),

            microphone = KhwabBlue,
            microphoneGlow = KhwabListening,

            buttonPrimary = KhwabBlue,
            buttonSecondary = KhwabGreen,
            buttonDanger = KhwabRed
        )
    }

    /**
     * Creates the animation profile.
     */
    private fun createAnimation(
        mode: ThemeMode
    ): ThemeAnimation {

        return when (mode) {

            ThemeMode.MINIMAL -> ThemeAnimation(
                transitionDuration = 150,
                fadeDuration = 150,
                scaleDuration = 150,
                backgroundAnimationSpeed = 0f,
                glowEnabled = false,
                glowIntensity = 0f,
                pulseEnabled = false,
                pulseDuration = 0,
                floatingEnabled = false,
                floatingSpeed = 0f,
                weatherAnimationEnabled = false,
                weatherIntensity = 0f,
                particleEnabled = false,
                particleDensity = 0f,
                blurEnabled = false,
                blurRadius = 0f,
                animationEnabled = false
            )

            ThemeMode.BALANCED -> ThemeAnimation(
                transitionDuration = 400,
                fadeDuration = 350,
                scaleDuration = 300,
                backgroundAnimationSpeed = 1f,
                glowEnabled = true,
                glowIntensity = 0.5f,
                pulseEnabled = true,
                pulseDuration = 1200,
                floatingEnabled = true,
                floatingSpeed = 1f,
                weatherAnimationEnabled = true,
                weatherIntensity = 0.5f,
                particleEnabled = true,
                particleDensity = 0.4f,
                blurEnabled = true,
                blurRadius = 8f,
                animationEnabled = true
            )

            ThemeMode.IMMERSIVE -> ThemeAnimation(
                transitionDuration = 700,
                fadeDuration = 600,
                scaleDuration = 500,
                backgroundAnimationSpeed = 2f,
                glowEnabled = true,
                glowIntensity = 1f,
                pulseEnabled = true,
                pulseDuration = 900,
                floatingEnabled = true,
                floatingSpeed = 2f,
                weatherAnimationEnabled = true,
                weatherIntensity = 1f,
                particleEnabled = true,
                particleDensity = 1f,
                blurEnabled = true,
                blurRadius = 16f,
                animationEnabled = true
            )
        }
    }
}