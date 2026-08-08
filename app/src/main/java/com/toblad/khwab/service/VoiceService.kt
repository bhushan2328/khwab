package com.toblad.khwab.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
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
    }

    private lateinit var speechManager: SpeechManager
    private lateinit var executionEngine: AndroidExecutionEngine
    private lateinit var floatingWindow: FloatingWindow
    private lateinit var integration: KhwabIntegration
    private var tts: TextToSpeech? = null

    /** True while audio recording is active. Toggled by mic-button tap. */
    @Volatile private var isListening = false

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

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
                speechManager.startListening()
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

        // If the service is not connected and the user tried a screen action,
        // speak a guidance message and open the accessibility settings.
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
            response.executionPlan?.let { plan ->
                Log.d(TAG, "Executing: ${plan.action}")
                withContext(Dispatchers.Main) {
                    floatingWindow.setState(AssistantState.EXECUTING)
                    AssistantStateManager.updateState(AssistantState.EXECUTING)
                }

                // Check if this is a screen action but accessibility is not enabled.
                val isScreenAction = plan.action in setOf(
                    "CLICK", "LONG_CLICK", "SCROLL", "TYPE_TEXT",
                    "GO_BACK", "GO_HOME", "READ_SCREEN", "FIND_ELEMENT", "FOCUS_ELEMENT"
                )
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
                } else {
                    val success = executionEngine.execute(plan)
                    Log.d(TAG, "Execution done: success=$success")
                    delay(STEP_DELAY_MS)
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
                RoomTemporaryKnowledgeRepository(
                    KhwabDatabase.getInstance(applicationContext).temporaryKnowledgeDao()
                ).deleteByKey(key)
                Log.d(TAG, "Deleted learned knowledge for key: $key")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete learned knowledge", e)
            }
        }
    }

    // ── Gemini direct fetch (voice path) ─────────────────────────────────────

    /**
     * Calls Gemini directly, waits for the answer, speaks it, and caches it
     * in the temporary knowledge store (30-day TTL) so chat and future voice
     * queries for the same topic get the cached answer without a new LLM call.
     */
    private suspend fun fetchGeminiAnswer(query: String): String? {
        return try {
            KhwabProvider.init(applicationContext)
            val client = KhwabProvider.llmClient ?: return null
            val llmService = LLMService(client)
            val promptBuilder = RelatedPromptBuilder()
            val extractor = LLMKnowledgeExtractor()
            val repo = RoomTemporaryKnowledgeRepository(
                KhwabDatabase.getInstance(applicationContext).temporaryKnowledgeDao()
            )

            // Pass the last N voice turns as conversation history so Gemini
            // can answer follow-up questions correctly.
            val history = buildVoiceHistory()
            val prompt = promptBuilder.buildPrimary(query, conversationHistory = history)
            val llmResponse = llmService.generate(prompt, "gemini-2.0-flash") ?: return null

            // Cache the answer — the chat screen and future voice queries benefit
            extractor.extract(query, llmResponse)?.let { record ->
                try { repo.save(record.key, record.value, 30, record.confidence) }
                catch (_: Exception) { /* non-fatal */ }
            }

            llmResponse.text.trim().takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini fetch failed: ${e.message}")
            null
        }
    }

    /**
     * Returns the last 6 voice turns as (role, text) pairs for Gemini context.
     */
    private fun buildVoiceHistory(): List<Pair<String, String>> {
        return try {
            val bridge = integration as? com.toblad.khwab.integration.internal.DefaultKhwabIntegration
                ?: return emptyList()
            val field = bridge.javaClass.getDeclaredField("coreBridge")
            field.isAccessible = true
            val coreBridge = field.get(bridge)
                as? com.toblad.khwab.integration.bridge.core.DefaultCoreBridge
                ?: return emptyList()
            val coordField = coreBridge.javaClass.getDeclaredField("coordinator")
            coordField.isAccessible = true
            val coordinator = coordField.get(coreBridge)
                as? com.toblad.khwab.core.brain.CognitiveCoordinator
                ?: return emptyList()
            coordinator.brain().conversationEngine.session.history
                .all()
                .takeLast(12)
                .mapIndexed { idx, text ->
                    val role = if (idx % 2 == 0) "User" else "Khwab"
                    role to text
                }
        } catch (_: Exception) {
            emptyList()
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
     * Strips markdown formatting characters so they are not read aloud.
     */
    private fun speak(text: String) {
        val clean = text
            .replace(Regex("\\*{1,3}"), "")           // **bold**, *italic*
            .replace(Regex("`+"), "")                  // `code`
            .replace(Regex("#{1,6} "), "")             // # headings
            .replace(Regex("(?m)^\\s*[-*+] "), "")    // - bullet / * bullet
            .replace(Regex("(?m)^\\s*\\d+\\. "), "")  // 1. numbered list
            .replace(Regex("__"), "")                  // __underline__
            .replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1") // [text](url) → text
            .replace(Regex("\\n{2,}"), ". ")           // blank lines → brief pause
            .replace("\n", ", ")                       // single newlines → comma pause
            .trim()
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "khwab_tts")
    }
}
