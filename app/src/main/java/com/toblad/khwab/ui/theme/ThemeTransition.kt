package com.toblad.khwab.ui.theme

/**
 * Defines how the UI should transition between themes.
 *
 * This class provides helper methods that determine whether
 * a transition should be animated and how long it should last.
 *
 * Future versions may include:
 * - Cross-fade transitions
 * - Gradient interpolation
 * - Animated weather transitions
 * - Background scene transitions
 */
object ThemeTransition {

    /**
     * Returns the animation duration (milliseconds)
     * for the current theme.
     */
    fun duration(
        state: ThemeState
    ): Int {
        return state.animation.transitionDuration
    }

    /**
     * Returns true if theme animations are enabled.
     */
    fun animationsEnabled(
        state: ThemeState
    ): Boolean {
        return state.animation.animationEnabled
    }

    /**
     * Determines whether a transition should be animated.
     */
    fun shouldAnimate(
        oldState: ThemeState,
        newState: ThemeState
    ): Boolean {

        if (!newState.animation.animationEnabled) {
            return false
        }

        return oldState != newState
    }

    /**
     * Determines whether the background should transition.
     */
    fun shouldAnimateBackground(
        oldState: ThemeState,
        newState: ThemeState
    ): Boolean {

        return oldState.time != newState.time ||
                oldState.weather != newState.weather ||
                oldState.environment != newState.environment ||
                oldState.season != newState.season
    }

    /**
     * Determines whether UI components should animate.
     */
    fun shouldAnimateComponents(
        oldState: ThemeState,
        newState: ThemeState
    ): Boolean {

        return oldState.palette != newState.palette
    }
}