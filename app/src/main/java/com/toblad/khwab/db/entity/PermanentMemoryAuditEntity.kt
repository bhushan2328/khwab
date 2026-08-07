package com.toblad.khwab.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Audit trail entry for a [PermanentMemoryEntity].
 */
@Entity(
    tableName = "permanent_memory_audit",
    foreignKeys = [
        ForeignKey(
            entity = PermanentMemoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["memory_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["memory_id"])]
)
data class PermanentMemoryAuditEntity(

    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "memory_id")
    val memoryId: String,

    @ColumnInfo(name = "operation")
    val operation: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "source")
    val source: String
)
