package com.toblad.khwab.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.toblad.khwab.db.dao.ChatMessageDao
import com.toblad.khwab.db.dao.PermanentMemoryDao
import com.toblad.khwab.db.dao.TemporaryKnowledgeDao
import com.toblad.khwab.db.entity.ChatMessageEntity
import com.toblad.khwab.db.entity.PermanentMemoryAuditEntity
import com.toblad.khwab.db.entity.PermanentMemoryEntity
import com.toblad.khwab.db.entity.PermanentMemoryVersionEntity
import com.toblad.khwab.db.entity.TemporaryKnowledgeEntity

@Database(
    entities = [
        TemporaryKnowledgeEntity::class,
        PermanentMemoryEntity::class,
        PermanentMemoryVersionEntity::class,
        PermanentMemoryAuditEntity::class,
        ChatMessageEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class KhwabDatabase : RoomDatabase() {

    abstract fun temporaryKnowledgeDao(): TemporaryKnowledgeDao

    abstract fun permanentMemoryDao(): PermanentMemoryDao

    abstract fun chatMessageDao(): ChatMessageDao

    companion object {

        @Volatile
        private var INSTANCE: KhwabDatabase? = null

        fun getInstance(context: Context): KhwabDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KhwabDatabase::class.java,
                    "khwab.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
