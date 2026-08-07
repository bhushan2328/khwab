package com.toblad.khwab.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for user-explicit permanent memory.
 *
 * Mirrors the in-memory model of [com.toblad.khwab.core.memory.model.Memory]
 * with [com.toblad.khwab.core.memory.model.MemoryType.PERMANENT].
 */
@Entity(
    tableName = "permanent_memory",
    indices = [Index(value = ["subject"], unique = true)]
)
data class PermanentMemoryEntity(

    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "subject")
    val subject: String,

    @ColumnInfo(name = "value")
    val value: String,

    @ColumnInfo(name = "category")
    val category: String = "OTHER",

    @ColumnInfo(name = "confidence")
    val confidence: String = "EXPLICIT",

    @ColumnInfo(name = "source")
    val source: String = "USER",

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "current_version")
    val currentVersion: Int = 1,

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null
)
