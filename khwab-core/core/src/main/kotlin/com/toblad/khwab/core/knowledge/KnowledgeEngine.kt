package com.toblad.khwab.core.knowledge

import com.toblad.khwab.core.memory.model.MemoryCategory
import com.toblad.khwab.core.memory.model.Memory
import com.toblad.khwab.core.memory.permanent.PermanentMemory
import com.toblad.khwab.core.memory.permanent.internal.RamPermanentMemory

/**
 * Central API for knowledge operations.
 *
 * Legacy API preserved while using the new Memory subsystem.
 */
class KnowledgeEngine(

    private val memory: PermanentMemory = RamPermanentMemory()

) {

    fun remember(
        key: String,
        value: String,
        type: KnowledgeType = KnowledgeType.USER_PREFERENCE
    ): KnowledgeRecord {

        val saved = memory.create(
            Memory.createPermanent(
                subject = key,
                value = value,
                category = MemoryCategory.PREFERENCE
            )
        )

        return KnowledgeRecord(
            key = saved.subject,
            value = saved.value,
            type = type
        )
    }

    fun recall(
        key: String
    ): KnowledgeRecord? {

        return memory.findDuplicate(key)?.let {
            KnowledgeRecord(
                key = it.subject,
                value = it.value,
                type = KnowledgeType.USER_PREFERENCE
            )
        }
    }

    fun forget(
        key: String
    ): Boolean {

        val existing = memory.findDuplicate(key)
            ?: return false

        return memory.delete(existing.id)
    }

    fun records(): List<KnowledgeRecord> {

        return memory
            .all()
            .map {
                KnowledgeRecord(
                    key = it.subject,
                    value = it.value,
                    type = KnowledgeType.USER_PREFERENCE
                )
            }
    }
}
