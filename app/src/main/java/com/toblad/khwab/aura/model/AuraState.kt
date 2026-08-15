package com.toblad.khwab.aura.model

/**
 * Lifecycle state of the Unity Aura.
 *
 * Used by AuraDebugConsole and AuraBridge to represent the
 * current activation status of the Aura environment.
 */
enum class AuraState {
    /** Aura is disabled. Unity environment roots are hidden. */
    OFF,

    /** Aura is starting up — UnityAuraBridge has sent ActivateAura. */
    STARTING,

    /** Aura is fully operational. */
    ACTIVE,

    /** Aura is shutting down. */
    STOPPING,

    /** Aura encountered an initialization or runtime error. */
    ERROR
}
