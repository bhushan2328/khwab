package com.toblad.khwab

import android.app.Application
import com.toblad.khwab.background.KnowledgeCleanupWorker
import com.toblad.khwab.di.KhwabProvider

/**
 * Application entry point.
 *
 * Responsibilities:
 *   1. Initialise [KhwabProvider] (builds Room DB, reads API key, wires integration).
 *   2. Schedule the daily [KnowledgeCleanupWorker] to purge expired temporary knowledge.
 */
class KhwabApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialise the core dependency graph with application context.
        KhwabProvider.init(this)

        // Schedule once-daily cleanup of expired temporary knowledge records.
        KnowledgeCleanupWorker.schedule(this)
    }
}
