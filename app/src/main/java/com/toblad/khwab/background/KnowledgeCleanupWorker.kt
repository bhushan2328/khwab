package com.toblad.khwab.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.toblad.khwab.db.KhwabDatabase
import com.toblad.khwab.db.repository.RoomTemporaryKnowledgeRepository
import com.toblad.khwab.logging.LogModule
import com.toblad.khwab.logging.Logger
import java.util.concurrent.TimeUnit

/**
 * Periodic WorkManager [CoroutineWorker] that removes expired temporary knowledge.
 *
 * Runs once per day. Requires no network — operates purely on local Room DB.
 * Enqueue via [schedule] once from Application.onCreate.
 */
class KnowledgeCleanupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "KnowledgeCleanup"
        private const val UNIQUE_NAME = "knowledge_cleanup_daily"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<KnowledgeCleanupWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            ).build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    UNIQUE_NAME,
                    androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                    request
                )

            Logger.debug(LogModule.SYSTEM, "Cleanup worker scheduled")
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val repo = RoomTemporaryKnowledgeRepository(
                KhwabDatabase.getInstance(applicationContext).temporaryKnowledgeDao()
            )
            val deleted = repo.purgeExpired()
            Logger.debug(LogModule.SYSTEM, "Cleanup: deleted $deleted expired records")
            Result.success()
        } catch (e: Exception) {
            Logger.error(LogModule.SYSTEM, "Cleanup failed: ${e.message}", e)
            Result.failure()
        }
    }
}
