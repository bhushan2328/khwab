package com.toblad.khwab.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for [com.toblad.khwab.db.entity.TemporaryKnowledgeEntity].
 *
 * All retrieval queries filter by [expiresAt] > now to exclude expired records.
 */
@Dao
interface TemporaryKnowledgeDao {

    /**
     * Insert or replace a temporary knowledge record.
     * Using REPLACE means re-acquisition refreshes the TTL automatically.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: com.toblad.khwab.db.entity.TemporaryKnowledgeEntity)

    /**
     * Find a non-expired record whose [queryKey] exactly matches [key] (case-insensitive).
     */
    @Query(
        "SELECT * FROM temporary_knowledge " +
        "WHERE LOWER(query_key) = LOWER(:key) AND expires_at > :now " +
        "LIMIT 1"
    )
    suspend fun findExact(key: String, now: Long): com.toblad.khwab.db.entity.TemporaryKnowledgeEntity?

    /**
     * Find non-expired records whose [queryKey] or [value] contains [query] (case-insensitive).
     */
    @Query(
        "SELECT * FROM temporary_knowledge " +
        "WHERE (LOWER(query_key) LIKE '%' || LOWER(:query) || '%' " +
        "    OR LOWER(value) LIKE '%' || LOWER(:query) || '%') " +
        "AND expires_at > :now " +
        "ORDER BY confidence DESC, created_at DESC"
    )
    suspend fun search(query: String, now: Long): List<com.toblad.khwab.db.entity.TemporaryKnowledgeEntity>

    /**
     * Delete all records whose [expiresAt] is before [now].
     */
    @Query("DELETE FROM temporary_knowledge WHERE expires_at <= :now")
    suspend fun deleteExpired(now: Long): Int

    /**
     * Delete all records matching a specific [queryKey] (for FORGET_LEARNED).
     */
    @Query("DELETE FROM temporary_knowledge WHERE LOWER(query_key) LIKE '%' || LOWER(:key) || '%'")
    suspend fun deleteByKey(key: String)

    /**
     * Observe all non-expired records as a Flow (for reactive UI updates).
     */
    @Query("SELECT * FROM temporary_knowledge WHERE expires_at > :now ORDER BY created_at DESC")
    fun observeAll(now: Long): Flow<List<com.toblad.khwab.db.entity.TemporaryKnowledgeEntity>>
}
