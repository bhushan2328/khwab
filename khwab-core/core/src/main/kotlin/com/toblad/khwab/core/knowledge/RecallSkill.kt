package com.toblad.khwab.core.knowledge

/**
 * Retrieves stored knowledge from the KnowledgeStore.
 */
class RecallSkill(

    private val store: KnowledgeStore

) {

    fun recall(key: String): KnowledgeRecord? {
        return store.find(key)
    }
}
