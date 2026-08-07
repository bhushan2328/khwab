package com.toblad.khwab.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for ChatGPT-derived temporary knowledge.
 *
 * Records expire after [expiresAt] and are cleaned up by
 * [com.toblad.khwab.background.KnowledgeCleanupWorker].
 */
@Entity(
    tableName = "temporary_knowledge",
    indices = [
        Index(value = ["query_key"]),
        Index(value = ["expires_at"])
    ]
)
data class TemporaryKnowledgeEntity(

    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "query_key")
    val queryKey: String,

    @ColumnInfo(name = "value")
    val value: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    /** Epoch ms after which this record is considered expired. */
    @ColumnInfo(name = "expires_at")
    val expiresAt: Long,

    @ColumnInfo(name = "source")
    val source: String = "LEARNED",

    @ColumnInfo(name = "confidence")
    val confidence: Float = 0.75f
)
