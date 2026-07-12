package com.toblad.khwab.core.memory.id

import com.toblad.khwab.core.memory.model.MemoryId
import java.util.UUID

/**
 * Generates unique IDs for Memory objects.
 *
 * All Memory IDs should be created through this generator so the
 * generation strategy can be changed later (UUID → ULID, etc.).
 */
object MemoryIdGenerator {

    fun next(): MemoryId {
        return MemoryId(UUID.randomUUID().toString())
    }
}
