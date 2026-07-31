package com.toblad.khwab.aura.system

import com.toblad.khwab.aura.engine.FrameClock
import com.toblad.khwab.aura.world.AuraWorld

/**
 * Base interface implemented by every Aura runtime system.
 */
interface AuraSystem {

    /**
     * Updates the world for one engine frame.
     */
    fun update(
        world: AuraWorld,
        clock: FrameClock
    )
}
