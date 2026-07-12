package com.toblad.khwab.core.knowledge

/**
 * Stores user knowledge into the KnowledgeStore.
 */
class RememberSkill(

    private val store: KnowledgeStore

) {

    fun remember(
        key: String,
        value: String,
        type: KnowledgeType = KnowledgeType.USER_PREFERENCE
    ): KnowledgeRecord {

        val record = KnowledgeRecord(
            key = key,
            value = value,
            type = type
        )

        store.save(record)

        return record
    }
}
