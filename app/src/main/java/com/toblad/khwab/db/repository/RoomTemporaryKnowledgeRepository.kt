package com.toblad.khwab.db.repository

import com.toblad.khwab.core.knowledge.KnowledgeRecord
import com.toblad.khwab.core.knowledge.KnowledgeSource
import com.toblad.khwab.core.knowledge.KnowledgeTier
import com.toblad.khwab.core.knowledge.KnowledgeType
import com.toblad.khwab.core.knowledge.LocalSearchResult
import com.toblad.khwab.core.knowledge.TemporaryKnowledgeRepository
import com.toblad.khwab.db.dao.TemporaryKnowledgeDao
import com.toblad.khwab.db.entity.TemporaryKnowledgeEntity
import java.util.UUID

/**
 * Room-backed implementation of [TemporaryKnowledgeRepository].
 *
 * Retrieval strategy (priority by confidence):
 *   1. Exact case-insensitive key match → confidence 0.80
 *   2. Keyword (LIKE) match on key or value → confidence 0.65
 */
class RoomTemporaryKnowledgeRepository(
    private val dao: TemporaryKnowledgeDao
) : TemporaryKnowledgeRepository {

    override suspend fun save(
        key: String,
        value: String,
        ttlDays: Int,
        confidence: Float
    ) {
        val now = System.currentTimeMillis()
        val expiresAt = now + ttlDays.toLong() * 24L * 60L * 60L * 1_000L
        val entity = TemporaryKnowledgeEntity(
            id = UUID.randomUUID().toString(),
            queryKey = key.trim().lowercase(),
            value = value.trim(),
            createdAt = now,
            expiresAt = expiresAt,
            confidence = confidence
        )
        dao.upsert(entity)
    }

    override suspend fun search(query: String): LocalSearchResult {
        val now = System.currentTimeMillis()
        val normKey = query.trim().lowercase()

        // 1. Exact match (highest confidence)
        val exact = dao.findExact(normKey, now)
        if (exact != null) {
            return LocalSearchResult(
                record = exact.toKnowledgeRecord(),
                confidence = 0.80f,
                tier = KnowledgeTier.TEMPORARY
            )
        }

        // 2. Keyword match (lower confidence)
        val keyword = dao.search(normKey, now).firstOrNull()
        if (keyword != null) {
            return LocalSearchResult(
                record = keyword.toKnowledgeRecord(),
                confidence = 0.65f,
                tier = KnowledgeTier.TEMPORARY
            )
        }

        return LocalSearchResult.NONE
    }

    override suspend fun deleteByKey(key: String) {
        dao.deleteByKey(key)
    }

    override suspend fun purgeExpired(): Int {
        return dao.deleteExpired(System.currentTimeMillis())
    }

    private fun TemporaryKnowledgeEntity.toKnowledgeRecord(): KnowledgeRecord =
        KnowledgeRecord(
            key = queryKey,
            value = value,
            type = KnowledgeType.FACT,
            source = KnowledgeSource.LEARNED,
            confidence = confidence,
            createdAt = createdAt,
            updatedAt = createdAt
        )
}
