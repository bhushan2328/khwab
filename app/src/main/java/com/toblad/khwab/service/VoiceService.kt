package com.toblad.khwab.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import com.toblad.khwab.background.KnowledgeAcquisitionWorker
import com.toblad.khwab.db.KhwabDatabase
import com.toblad.khwab.db.repository.RoomTemporaryKnowledgeRepository
import com.toblad.khwab.di.KhwabProvider
import com.toblad.khwab.executor.AndroidExecutionEngine
import com.toblad.khwab.integration.api.KhwabIntegration
import com.toblad.khwab.integration.api.request.IntegrationRequest
import com.toblad.khwab.logging.LogModule
import com.toblad.khwab.logging.Logger
import com.toblad.khwab.overlay.FloatingWindow
import com.toblad.khwab.speech.SpeechManager
import com.toblad.khwab.state.AssistantState
import com.toblad.khwab.state.AssistantStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class VoiceService : Service() {

    companion object {
        private const val TAG = "VoiceService"
        private const val NOTIFICATION_ID = 1001
    }

    private lateinit var speechManager: SpeechManager
    private lateinit var executionEngine: AndroidExecutionEngine
    private lateinit var floatingWindow: FloatingWindow
    private lateinit var integration: KhwabIntegration
    private var tts: TextToSpeech? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "VoiceService created")

        KhwabProvider.init(applicationContext)
        integration = KhwabProvider.integration

        executionEngine = AndroidExecutionEngine(this)
        floatingWindow = FloatingWindow(this)
        speechManager = SpeechManager(this)

        // Initialise TTS engine
        tts = TextToSpeech(applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                Log.d(TAG, "TTS initialised")
            } else {
                Log.w(TAG, "TTS initialisation failed (status=$status)")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // startForeground must be called immediately on the main thread — before
        // any heavy work — to satisfy the 5-second foreground service deadline.
        NotificationHelper.createNotificationChannel(this)
        startForeground(NOTIFICATION_ID, NotificationHelper.createNotification(this))

        // Show the overlay immediately so the user sees it right away.
        floatingWindow.show()
        floatingWindow.setState(AssistantState.READY)
        AssistantStateManager.updateState(AssistantState.READY)

        // Move all heavy work (ONNX model load + audio record loop) to a background
        // thread. Doing this on the main thread causes an ANR and the service crashes
        // before the overlay ever becomes visible.
        serviceScope.launch {
            try {
                Log.d(TAG, "Registering recognition listener")

                speechManager.setRecognitionListener { result ->

                    Log.d("Sherpa", result.text)

                    // listener fires on the AudioRecorder background thread —
                    // switch to Main for UI updates, then to IO for the network call
                    serviceScope.launch(Dispatchers.Main) {
                        floatingWindow.setState(AssistantState.THINKING)
                        AssistantStateManager.updateState(AssistantState.THINKING)
                    }

                    serviceScope.launch {
                        // Capture the current screen before processing.
                        // AccessibilityTreeMapper returns null when the service is not
                        // enabled — the pipeline handles null gracefully.
                        val screenSnapshot = AccessibilityTreeMapper.capture()
                        if (screenSnapshot != null) {
                            Logger.info(
                                LogModule.ACCESSIBILITY,
                                "Screen captured: pkg=${screenSnapshot.packageName} " +
                                "elements=${screenSnapshot.allElements().size}"
                            )
                        }

                        val response = try {
                            integration.process(
                                IntegrationRequest(
                                    input = result.text,
                                    screenContext = screenSnapshot
                                )
                            )
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                floatingWindow.setState(AssistantState.ERROR)
                                AssistantStateManager.updateState(AssistantState.ERROR)
                            }
                            Log.e(TAG, "Integration error", e)
                            return@launch
                        }

                        // Execute Android-side command (open app, accessibility action, etc.)
                        response.executionPlan?.let { plan ->
                            Log.d("Khwab", "Executing: ${plan.action}")
                            withContext(Dispatchers.Main) {
                                floatingWindow.setState(AssistantState.EXECUTING)
                                AssistantStateManager.updateState(AssistantState.EXECUTING)
                            }
                            executionEngine.execute(plan)
                            Log.d("Khwab", "Execution done")
                        }

                        // For READ_SCREEN: if the AccessibilityService captured screen text,
                        // use that as the spoken response (overrides any generic Core message).
                        val screenReadText = KhwabAccessibilityService.instance.get()
                            ?.lastScreenReadResult
                            ?.also {
                                // Consume the result so it isn't re-spoken next turn.
                                KhwabAccessibilityService.instance.get()?.lastScreenReadResult = null
                            }

                        // Speak the response text back to the user.
                        // Priority: screen-read text > Core response message.
                        val responseText = screenReadText?.takeIf { it.isNotBlank() }
                            ?: response.message
                        if (!responseText.isNullOrBlank() && response.success) {
                            withContext(Dispatchers.Main) {
                                floatingWindow.setState(AssistantState.SPEAKING)
                                AssistantStateManager.updateState(AssistantState.SPEAKING)
                            }
                            speak(responseText)
                        }

                        // Schedule background knowledge acquisition if needed
                        if (response.requiresAcquisition) {
                            val query = response.acquisitionQuery ?: result.text
                            Log.d(TAG, "Scheduling knowledge acquisition for: $query")
                            KnowledgeAcquisitionWorker.enqueue(this@VoiceService, query)
                        }

                        // Delete learned knowledge if user asked to forget it
                        response.forgetLearnedKey?.let { key ->
                            try {
                                RoomTemporaryKnowledgeRepository(
                                    KhwabDatabase.getInstance(applicationContext).temporaryKnowledgeDao()
                                ).deleteByKey(key)
                                Log.d(TAG, "Deleted learned knowledge for key: $key")
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to delete learned knowledge", e)
                            }
                        }

                        withContext(Dispatchers.Main) {
                            floatingWindow.setState(AssistantState.LISTENING)
                            AssistantStateManager.updateState(AssistantState.LISTENING)
                        }
                    }
                }

                Log.d(TAG, "Initializing Sherpa (background thread)")
                speechManager.initialize()

                Log.d(TAG, "Starting listening")
                withContext(Dispatchers.Main) {
                    floatingWindow.setState(AssistantState.LISTENING)
                    AssistantStateManager.updateState(AssistantState.LISTENING)
                }
                speechManager.startListening()

                Log.d(TAG, "VoiceService started successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start VoiceService", e)
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    AssistantStateManager.updateState(AssistantState.ERROR)
                    Toast.makeText(
                        this@VoiceService,
                        "VoiceService Error:\n${e.javaClass.simpleName}\n${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "Stopping VoiceService")

        // Always reset state to STOPPED so the HomeScreen reflects reality
        // even when the OS kills the service without stopAssistant() being called.
        AssistantStateManager.updateState(AssistantState.STOPPED)

        try { speechManager.release() } catch (e: Exception) {
            Log.e(TAG, "Failed to release SpeechManager", e)
        }
        try { floatingWindow.hide() } catch (e: Exception) {
            Log.e(TAG, "Failed to hide FloatingWindow", e)
        }
        try { tts?.stop(); tts?.shutdown() } catch (e: Exception) {
            Log.e(TAG, "Failed to shutdown TTS", e)
        }

        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Speaks [text] via Android TTS.
     * Strips markdown formatting characters so they are not read aloud.
     */
    private fun speak(text: String) {
        val clean = text
            .replace(Regex("\\*+"), "")   // remove ** and *
            .replace(Regex("`+"), "")     // remove backticks
            .replace(Regex("#+ "), "")    // remove heading markers
            .trim()
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "khwab_tts")
    }
}
