package com.toblad.khwab.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.toblad.khwab.db.KhwabDatabase
import com.toblad.khwab.db.repository.RoomTemporaryKnowledgeRepository
import com.toblad.khwab.di.KhwabProvider
import com.toblad.khwab.executor.AndroidExecutionEngine
import com.toblad.khwab.integration.api.KhwabIntegration
import com.toblad.khwab.integration.api.request.IntegrationRequest
import com.toblad.khwab.integration.llm.LLMService
import com.toblad.khwab.integration.openai.LLMKnowledgeExtractor
import com.toblad.khwab.integration.openai.RelatedPromptBuilder
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class VoiceService : Service() {

    companion object {
        private const val TAG = "VoiceService"
        private const val NOTIFICATION_ID = 1001
        /** Pause between consecutive plan steps so Android UI has time to react. */
        private const val STEP_DELAY_MS = 600L
        /** Gemini model used for voice-path direct fetches. */
        const val GEMINI_MODEL = "gemini-2.0-flash"
    }

    private lateinit var speechManager: SpeechManager
    private lateinit var executionEngine: AndroidExecutionEngine
    private lateinit var floatingWindow: FloatingWindow
    private lateinit var integration: KhwabIntegration
    private var tts: TextToSpeech? = null

    /** True while audio recording is active. Toggled by mic-button tap. */
    @Volatile private var isListening = false

    /** True while TTS is currently speaking. Used to suppress mic re-start until done. */
    @Volatile private var isSpeaking = false

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── Lazy singletons for Gemini fetch (allocated once, reused on every call) ──

    private val llmService by lazy { LLMService(KhwabProvider.llmClient!!) }
    private val promptBuilder by lazy { RelatedPromptBuilder() }
    private val knowledgeExtractor by lazy { LLMKnowledgeExtractor() }
    private val knowledgeRepo by lazy {
        RoomTemporaryKnowledgeRepository(
            KhwabDatabase.getInstance(applicationContext).temporaryKnowledgeDao()
        )
    }

    override fun onCreate() {
        super.onCreate()

        // Reset stale state from any prior crashed session before anything else runs.
        AssistantStateManager.updateState(AssistantState.STOPPED)

        Log.d(TAG, "VoiceService created")

        KhwabProvider.init(applicationContext)
        integration = KhwabProvider.integration

        executionEngine = AndroidExecutionEngine(this)

        // Pass mic-tap toggle as a lambda — FloatingWindow knows nothing about
        // VoiceService internals; it just calls this when the button is tapped.
        floatingWindow = FloatingWindow(this, onMicTap = ::toggleListening)

        speechManager = SpeechManager(this)

        tts = TextToSpeech(applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()

                // ── Echo-loop prevention: pause mic while speaking, resume after ──
                // When TTS finishes an utterance the mic restarts automatically so
                // the assistant does not hear its own voice as a new command.
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isSpeaking = true
                    }
                    override fun onDone(utteranceId: String?) {
                        isSpeaking = false
                        // Resume listening only if the user had the mic active.
                        if (isListening) {
                            speechManager.startListening()
                            Log.d(TAG, "TTS done — mic resumed")
                        }
                    }
                    @Deprecated("Deprecated in API 21")
                    override fun onError(utteranceId: String?) {
                        isSpeaking = false
                        if (isListening) speechManager.startListening()
                    }
                })
                Log.d(TAG, "TTS initialised")
            } else {
                Log.w(TAG, "TTS initialisation failed (status=$status)")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        NotificationHelper.createNotificationChannel(this)
        startForeground(NOTIFICATION_ID, NotificationHelper.createNotification(this))

        floatingWindow.show()
        floatingWindow.setState(AssistantState.READY)
        AssistantStateManager.updateState(AssistantState.READY)

        serviceScope.launch {
            try {
                Log.d(TAG, "Registering recognition listener")

                speechManager.setRecognitionListener { result ->
                    Log.d("Sherpa", result.text)

                    serviceScope.launch(Dispatchers.Main) {
                        floatingWindow.setState(AssistantState.THINKING)
                        AssistantStateManager.updateState(AssistantState.THINKING)
                    }

                    serviceScope.launch {
                        processVoiceInput(result.text)

                        // Return to LISTENING state only when currently active.
                        withContext(Dispatchers.Main) {
                            if (isListening) {
                                floatingWindow.setState(AssistantState.LISTENING)
                                AssistantStateManager.updateState(AssistantState.LISTENING)
                            } else {
                                floatingWindow.setState(AssistantState.READY)
                                AssistantStateManager.updateState(AssistantState.READY)
                            }
                        }
                    }
                }

                Log.d(TAG, "Initializing Sherpa (background thread)")
                speechManager.initialize()

                // Start in READY (not listening) — user must tap the mic to begin.
                withContext(Dispatchers.Main) {
                    floatingWindow.setState(AssistantState.READY)
                    AssistantStateManager.updateState(AssistantState.READY)
                }

                Log.d(TAG, "VoiceService ready — tap mic to start listening")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start VoiceService", e)
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    AssistantStateManager.updateState(AssistantState.ERROR)
                    floatingWindow.setState(AssistantState.ERROR)
                }
                // Speak the error so it reaches the user even over a fullscreen app.
                speak("Khwab failed to start. ${e.javaClass.simpleName}: ${e.message}")
                stopSelf()
            }
        }

        return START_STICKY
    }

    // ── Mic toggle ────────────────────────────────────────────────────────────

    /**
     * Called by [FloatingWindow] when the mic button is tapped.
     * Switches between LISTENING and READY (paused) states.
     */
    private fun toggleListening() {
        serviceScope.launch {
            if (isListening) {
                isListening = false
                speechManager.stopListening()
                withContext(Dispatchers.Main) {
                    floatingWindow.setState(AssistantState.READY)
                    AssistantStateManager.updateState(AssistantState.READY)
                }
                Log.d(TAG, "Mic paused by user")
            } else {
                isListening = true
                // Don't start mic if TTS is currently speaking — onDone will resume it.
                if (!isSpeaking) speechManager.startListening()
                withContext(Dispatchers.Main) {
                    floatingWindow.setState(AssistantState.LISTENING)
                    AssistantStateManager.updateState(AssistantState.LISTENING)
                }
                Log.d(TAG, "Mic activated by user")
            }
        }
    }

    // ── Core voice processing ─────────────────────────────────────────────────

    private suspend fun processVoiceInput(text: String) {

        // Capture the live screen snapshot before processing.
        val screenSnapshot = AccessibilityTreeMapper.capture()

        if (screenSnapshot == null) {
            Logger.info(LogModule.ACCESSIBILITY, "Accessibility service not connected")
        } else {
            Logger.info(
                LogModule.ACCESSIBILITY,
                "Screen captured: pkg=${screenSnapshot.packageName} " +
                "elements=${screenSnapshot.allElements().size}"
            )
        }

        val response = try {
            integration.process(
                IntegrationRequest(
                    input = text,
                    screenContext = screenSnapshot
                )
            )
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                floatingWindow.setState(AssistantState.ERROR)
                AssistantStateManager.updateState(AssistantState.ERROR)
            }
            Log.e(TAG, "Integration error", e)
            return
        }

        // ── Execute all plan steps in order ──────────────────────────────────
        if (response.success) {
            val plans = response.executionPlans.ifEmpty {
                listOfNotNull(response.executionPlan)
            }

            if (plans.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    floatingWindow.setState(AssistantState.EXECUTING)
                    AssistantStateManager.updateState(AssistantState.EXECUTING)
                }

                val screenActions = setOf(
                    "CLICK", "LONG_CLICK", "SCROLL", "TYPE_TEXT",
                    "GO_BACK", "GO_HOME", "READ_SCREEN", "FIND_ELEMENT", "FOCUS_ELEMENT"
                )

                for (plan in plans) {
                    Log.d(TAG, "Executing step: ${plan.action}")

                    val isScreenAction = plan.action in screenActions
                    if (isScreenAction && KhwabAccessibilityService.instance.get() == null) {
                        speak(
                            "Please enable Khwab in Settings, then Accessibility, " +
                            "to use screen actions."
                        )
                        withContext(Dispatchers.Main) {
                            val settingsIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(settingsIntent)
                        }
                        break
                    } else {
                        val success = executionEngine.execute(plan)
                        Log.d(TAG, "Step done: action=${plan.action} success=$success")

                        // Speak feedback when a screen action fails silently.
                        if (!success && isScreenAction) {
                            speak("Couldn't find that button. Try saying it differently.")
                        }

                        delay(STEP_DELAY_MS)
                    }
                }
            }
        }

        // ── READ_SCREEN: speak captured text ──────────────────────────────────
        val screenReadText = KhwabAccessibilityService.instance.get()
            ?.lastScreenReadResult
            ?.also { KhwabAccessibilityService.instance.get()?.lastScreenReadResult = null }

        // Priority: screen-read text > Core response message.
        val responseText = screenReadText?.takeIf { it.isNotBlank() } ?: response.message
        if (!responseText.isNullOrBlank() && response.success) {
            withContext(Dispatchers.Main) {
                floatingWindow.setState(AssistantState.SPEAKING)
                AssistantStateManager.updateState(AssistantState.SPEAKING)
            }
            speak(responseText)
        }

        // ── Gemini acquisition: call directly and speak the answer ────────────
        if (response.requiresAcquisition) {
            val query = response.acquisitionQuery ?: text
            Log.d(TAG, "Voice Gemini fetch for: $query")
            withContext(Dispatchers.Main) {
                floatingWindow.setState(AssistantState.THINKING)
                AssistantStateManager.updateState(AssistantState.THINKING)
            }
            val geminiAnswer = fetchGeminiAnswer(query)
            if (!geminiAnswer.isNullOrBlank()) {
                withContext(Dispatchers.Main) {
                    floatingWindow.setState(AssistantState.SPEAKING)
                    AssistantStateManager.updateState(AssistantState.SPEAKING)
                }
                speak(geminiAnswer)
            } else {
                speak("I couldn't find an answer right now. Please try again.")
            }
        }

        // ── Forget learned knowledge ──────────────────────────────────────────
        response.forgetLearnedKey?.let { key ->
            try {
                knowledgeRepo.deleteByKey(key)
                Log.d(TAG, "Deleted learned knowledge for key: $key")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete learned knowledge", e)
            }
        }
    }

    // ── Gemini direct fetch (voice path) ─────────────────────────────────────

    /**
     * Calls Gemini directly, waits for the answer, speaks it, and caches it
     * in the temporary knowledge store (30-day TTL).
     *
     * Uses lazy singleton objects — no allocations on repeated calls.
     */
    private suspend fun fetchGeminiAnswer(query: String): String? {
        return try {
            val history = integration.conversationHistory()
            val prompt = promptBuilder.buildPrimary(query, conversationHistory = history)
            val llmResponse = llmService.generate(prompt, GEMINI_MODEL) ?: return null

            knowledgeExtractor.extract(query, llmResponse)?.let { record ->
                try { knowledgeRepo.save(record.key, record.value, 30, record.confidence) }
                catch (_: Exception) { /* non-fatal */ }
            }

            llmResponse.text.trim().takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini fetch failed: ${e.message}")
            null
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onDestroy() {
        Log.d(TAG, "Stopping VoiceService")
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
     * Stops the mic first to prevent the echo loop — it resumes automatically
     * in the UtteranceProgressListener.onDone callback once speech finishes.
     * Strips markdown formatting so it is not read aloud.
     */
    private fun speak(text: String) {
        // Stop mic before speaking to prevent echo loop.
        if (isListening) speechManager.stopListening()

        val clean = text
            .replace(Regex("\\*{1,3}"), "")
            .replace(Regex("`+"), "")
            .replace(Regex("#{1,6} "), "")
            .replace(Regex("(?m)^\\s*[-*+] "), "")
            .replace(Regex("(?m)^\\s*\\d+\\. "), "")
            .replace(Regex("__"), "")
            .replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
            .replace(Regex("\\n{2,}"), ". ")
            .replace("\n", ", ")
            .trim()
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "khwab_tts")
    }
}
