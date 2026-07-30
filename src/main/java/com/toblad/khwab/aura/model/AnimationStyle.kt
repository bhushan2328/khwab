package com.toblad.khwab.aura.model

/**
 * Represents the overall animation behaviour
 * of the Aura scene.
 *
 * This controls the movement speed and feel
 * of visual elements such as clouds, particles,
 * celestial bodies and transitions.
 */
enum class AnimationStyle {

    /**
     * No animation.
     */
    NONE,

    /**
     * Slow, peaceful movement.
     */
    CALM,

    /**
     * Gentle movement with a light breeze.
     */
    BREEZY,

    /**
     * Moderate continuous motion.
     */
    NORMAL,

    /**
     * Strong wind and faster movement.
     */
    WINDY,

    /**
     * Rain animation.
     */
    RAIN,

    /**
     * Snow animation.
     */
    SNOW,

    /**
     * Storm animation with intense movement.
     */
    STORM
}
