package com.toblad.khwab.core.knowledge

/**
 * In-memory knowledge store.
 * This will later be backed by persistent storage.
 */
class KnowledgeStore {

    private val records = mutableMapOf<String, KnowledgeRecord>()

    fun save(record: KnowledgeRecord) {
        records[record.key] = record
    }

    fun find(key: String): KnowledgeRecord? {
        return records[key]
    }

    fun remove(key: String): Boolean {
        return records.remove(key) != null
    }

    fun all(): List<KnowledgeRecord> {
        return records.values.toList()
    }

    fun clear() {
        records.clear()
    }
}
