package com.toblad.khwab.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.toblad.khwab.db.entity.PermanentMemoryAuditEntity
import com.toblad.khwab.db.entity.PermanentMemoryEntity
import com.toblad.khwab.db.entity.PermanentMemoryVersionEntity

/**
 * Data access object for permanent user-explicit memory.
 */
@Dao
interface PermanentMemoryDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: PermanentMemoryEntity)

    @Update
    suspend fun update(entity: PermanentMemoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(entity: PermanentMemoryVersionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudit(entity: PermanentMemoryAuditEntity)

    @Query("SELECT * FROM permanent_memory WHERE id = :id AND is_deleted = 0")
    suspend fun findById(id: String): PermanentMemoryEntity?

    /** Case-insensitive exact subject match among non-deleted records. */
    @Query(
        "SELECT * FROM permanent_memory " +
        "WHERE LOWER(subject) = LOWER(:subject) AND is_deleted = 0 " +
        "LIMIT 1"
    )
    suspend fun findBySubject(subject: String): PermanentMemoryEntity?

    /** Keyword search in subject and value among non-deleted records. */
    @Query(
        "SELECT * FROM permanent_memory " +
        "WHERE (LOWER(subject) LIKE '%' || LOWER(:query) || '%' " +
        "    OR LOWER(value) LIKE '%' || LOWER(:query) || '%') " +
        "AND is_deleted = 0 " +
        "ORDER BY updated_at DESC"
    )
    suspend fun search(query: String): List<PermanentMemoryEntity>

    @Query("SELECT * FROM permanent_memory WHERE is_deleted = 0 ORDER BY updated_at DESC")
    suspend fun findAll(): List<PermanentMemoryEntity>

    @Query("SELECT * FROM permanent_memory_versions WHERE memory_id = :memoryId ORDER BY version ASC")
    suspend fun findVersions(memoryId: String): List<PermanentMemoryVersionEntity>

    @Query("SELECT * FROM permanent_memory_audit WHERE memory_id = :memoryId ORDER BY timestamp ASC")
    suspend fun findAudit(memoryId: String): List<PermanentMemoryAuditEntity>
}
