package com.toblad.khwab.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.toblad.khwab.db.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {

    /** Insert or replace a message (used for in-place Gemini answer updates). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: ChatMessageEntity)

    /** Insert or replace a list of messages at once. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<ChatMessageEntity>)

    /** Load all messages ordered oldest-first. */
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    suspend fun loadAll(): List<ChatMessageEntity>

    /** Observe all messages as a Flow — updates UI reactively. */
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun observeAll(): Flow<List<ChatMessageEntity>>

    /** Delete all messages (clear conversation). */
    @Query("DELETE FROM chat_messages")
    suspend fun deleteAll()

    /** Update text, status and state of a single message (Gemini in-place update). */
    @Query("UPDATE chat_messages SET text = :text, status = :status, state = :state WHERE id = :id")
    suspend fun updateContent(id: Long, text: String, status: String, state: String)
}
