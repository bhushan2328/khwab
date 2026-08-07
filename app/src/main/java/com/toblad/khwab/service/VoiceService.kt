package com.toblad.khwab.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.toblad.khwab.background.KnowledgeAcquisitionWorker
import com.toblad.khwab.db.KhwabDatabase
import com.toblad.khwab.db.repository.RoomTemporaryKnowledgeRepository
import com.toblad.khwab.di.KhwabProvider
import com.toblad.khwab.executor.AndroidExecutionEngine
import com.toblad.khwab.integration.api.KhwabIntegration
import com.toblad.khwab.integration.api.request.IntegrationRequest
import com.toblad.khwab.overlay.FloatingWindow
import com.toblad.khwab.speech.SpeechManager
import com.toblad.khwab.state.AssistantState
import com.toblad.khwab.state.AssistantStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class VoiceService : Service() {

    companion object {
        private const val TAG = "VoiceService"
        private const val NOTIFICATION_ID = 1001
    }

    private lateinit var speechManager: SpeechManager
    private lateinit var executionEngine: AndroidExecutionEngine
    private lateinit var floatingWindow: FloatingWindow
    private lateinit var integration: KhwabIntegration

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "VoiceService created")

        // Initialise the shared provider so the fully-wired integration
        // (Room DB + OpenAI) is available to voice mode as well.
        KhwabProvider.init(applicationContext)
        integration = KhwabProvider.integration

        executionEngine = AndroidExecutionEngine(this)
        floatingWindow = FloatingWindow(this)
        speechManager = SpeechManager(this)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        try {

            Log.d(TAG, "Starting foreground service")

            NotificationHelper.createNotificationChannel(this)

            startForeground(
                NOTIFICATION_ID,
                NotificationHelper.createNotification(this)
            )

            Log.d(TAG, "Showing floating window")

            floatingWindow.show()
            floatingWindow.setState(AssistantState.READY)
            AssistantStateManager.updateState(AssistantState.READY)

            Log.d(TAG, "Integration ready (Room + OpenAI wired via KhwabProvider)")

            speechManager.setRecognitionListener { result ->

                Log.d("Sherpa", result.text)

                floatingWindow.setState(AssistantState.THINKING)
                AssistantStateManager.updateState(AssistantState.THINKING)

                val response = try {
                    integration.process(
                        IntegrationRequest(
                            input = result.text
                        )
                    )
                } catch (e: Exception) {
                    floatingWindow.setState(AssistantState.ERROR)
                    Log.e(TAG, "Integration error", e)
                    return@setRecognitionListener
                }

                response.executionPlan?.let { plan ->

                    Log.d("Khwab", "Executing: ${plan.action}")

                    floatingWindow.setState(AssistantState.EXECUTING)
                    AssistantStateManager.updateState(AssistantState.EXECUTING)

                    val success = executionEngine.execute(plan)

                    Log.d("Khwab", "Execution Success: $success")
                }

                // Schedule background knowledge acquisition if needed
                if (response.requiresAcquisition) {
                    val query = response.acquisitionQuery ?: result.text
                    Log.d(TAG, "Scheduling knowledge acquisition for: $query")
                    KnowledgeAcquisitionWorker.enqueue(this, query)
                }

                // Delete learned knowledge if user asked to forget it
                response.forgetLearnedKey?.let { key ->
                    serviceScope.launch {
                        try {
                            val repo = RoomTemporaryKnowledgeRepository(
                                KhwabDatabase.getInstance(applicationContext).temporaryKnowledgeDao()
                            )
                            repo.deleteByKey(key)
                            Log.d(TAG, "Deleted learned knowledge for key: $key")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to delete learned knowledge", e)
                        }
                    }
                }

                floatingWindow.setState(AssistantState.LISTENING)
                AssistantStateManager.updateState(AssistantState.LISTENING)
            }

            Log.d(TAG, "Initializing Sherpa")

            speechManager.initialize()

            Log.d(TAG, "Starting listening")

            floatingWindow.setState(AssistantState.LISTENING)
            AssistantStateManager.updateState(AssistantState.LISTENING)
            speechManager.startListening()

            Log.d(TAG, "VoiceService started successfully")

        } catch (e: Exception) {

            Log.e(TAG, "Failed to start VoiceService", e)
            e.printStackTrace()

            Toast.makeText(
                this,
                "VoiceService Error:\n${e.javaClass.simpleName}\n${e.message}",
                Toast.LENGTH_LONG
            ).show()

            stopSelf()
        }

        return START_STICKY
    }

    override fun onDestroy() {

        Log.d(TAG, "Stopping VoiceService")

        try {
            speechManager.release()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release SpeechManager", e)
        }

        try {
            floatingWindow.hide()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hide FloatingWindow", e)
        }

        serviceScope.cancel()

        stopForeground(STOP_FOREGROUND_REMOVE)

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}