package com.toblad.khwab.environment

import android.content.Context
import com.toblad.khwab.aura.AuraBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Keeps Aura's weather in sync with reality while Aura is
 * active, by re-syncing on the interval configured in
 * settings (default: every 5 minutes). Started on activate(),
 * stopped on deactivate().
 */
class AuraSyncScheduler(
    context: Context
) {

    private val environmentSync = AuraEnvironmentSync(context)

    private val scope = CoroutineScope(Dispatchers.Main)

    private var job: Job? = null

    fun start() {

        if (job?.isActive == true) return

        job = scope.launch {
            while (true) {
                val minutes = AuraBridge.getConfig()
                    .refreshIntervalMinutes
                    .coerceAtLeast(1)

                delay(minutes * 60_000L)
                environmentSync.sync()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}