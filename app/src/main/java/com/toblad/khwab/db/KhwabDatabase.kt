package com.toblad.khwab.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    exportSchema = true
)
abstract class KhwabDatabase : RoomDatabase() {

    abstract fun temporaryKnowledgeDao(): TemporaryKnowledgeDao

    abstract fun permanentMemoryDao(): PermanentMemoryDao

    abstract fun chatMessageDao(): ChatMessageDao

    companion object {

        @Volatile
        private var INSTANCE: KhwabDatabase? = null

        /**
         * Migration from schema version 1 → 2.
         *
         * Version 2 added [ChatMessageEntity] (chat_messages table).
         * All other tables were already present in v1.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS chat_messages (
                        id INTEGER NOT NULL PRIMARY KEY,
                        text TEXT NOT NULL,
                        sender TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        status TEXT NOT NULL DEFAULT 'SENT',
                        state TEXT NOT NULL DEFAULT 'COMPLETE'
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): KhwabDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KhwabDatabase::class.java,
                    "khwab.db"
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
