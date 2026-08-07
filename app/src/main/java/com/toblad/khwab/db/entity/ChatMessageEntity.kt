package com.toblad.khwab.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persists a single chat message so conversations survive app restarts.
 */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(

    @PrimaryKey
    val id: Long,

    @ColumnInfo(name = "text")
    val text: String,

    @ColumnInfo(name = "sender")
    val sender: String,          // "USER" or "KHWAB"

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "status")
    val status: String = "SENT",

    @ColumnInfo(name = "state")
    val state: String = "COMPLETE"
)
