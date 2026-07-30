package com.toblad.khwab.aura.model

/**
 * Represents the cloud appearance used by Aura.
 *
 * This model controls cloud density and
 * rendering style.
 */
enum class CloudStyle {

    /**
     * No visible clouds.
     */
    CLEAR,

    /**
     * A few small clouds.
     */
    LIGHT,

    /**
     * Partly cloudy sky.
     */
    PARTLY_CLOUDY,

    /**
     * Mostly cloudy.
     */
    CLOUDY,

    /**
     * Thick overcast clouds.
     */
    OVERCAST,

    /**
     * Dark rain clouds.
     */
    RAIN,

    /**
     * Heavy storm clouds.
     */
    STORM,

    /**
     * Fast-moving windy clouds.
     */
    WINDY,

    /**
     * Low fog clouds.
     */
    FOG
}
