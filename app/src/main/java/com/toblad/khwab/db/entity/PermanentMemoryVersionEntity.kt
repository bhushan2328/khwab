package com.toblad.khwab.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Version history entry for a [PermanentMemoryEntity].
 */
@Entity(
    tableName = "permanent_memory_versions",
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
data class PermanentMemoryVersionEntity(

    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "memory_id")
    val memoryId: String,

    @ColumnInfo(name = "version")
    val version: Int,

    @ColumnInfo(name = "value")
    val value: String,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
