package com.toblad.khwab.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.toblad.khwab.chat.model.ChatMessage
import com.toblad.khwab.chat.model.MessageState
import com.toblad.khwab.chat.model.MessageStatus
import com.toblad.khwab.chat.model.Sender
import com.toblad.khwab.db.KhwabDatabase
import com.toblad.khwab.db.entity.ChatMessageEntity
import com.toblad.khwab.db.repository.RoomTemporaryKnowledgeRepository
import com.toblad.khwab.di.KhwabProvider
import com.toblad.khwab.executor.AndroidExecutionEngine
import com.toblad.khwab.integration.api.KhwabIntegration
import com.toblad.khwab.integration.api.request.IntegrationRequest
import com.toblad.khwab.integration.llm.LLMService
import com.toblad.khwab.integration.llm.providers.LLMKnowledgeExtractor
import com.toblad.khwab.integration.llm.providers.RelatedPromptBuilder
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
import java.util.concurrent.atomic.AtomicLong

class VoiceService : Service() {

    companion object {
        private const val TAG = "VoiceService"
        private const val NOTIFICATION_ID = 1001
        /** Pause between consecutive plan steps that do NOT change the screen. */
        private const val STEP_DELAY_MS = 600L
        /**
         * Extra wait after a step that triggers a screen transition
         * (app launch, click, back, etc.) so the new UI has time to settle
         * before AccessibilityTreeMapper captures the fresh snapshot.
         */
        private const val SCREEN_SETTLE_MS = 1200L
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
    private val chatMessageDao by lazy {
        KhwabDatabase.getInstance(applicationContext).chatMessageDao()
    }
    private val msgIdCounter = AtomicLong(System.currentTimeMillis())
    private fun nextMsgId() = msgIdCounter.incrementAndGet()

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

    /**
     * When a destructive confirmation is pending this holds the original command text.
     * The next "yes" response executes it; any other response cancels it.
     */
    @Volatile private var pendingConfirmationCommand: String? = null

    private suspend fun processVoiceInput(text: String) {

        // ── Confirmation reply handling ───────────────────────────────────────
        // If the previous response required confirmation, the user's current utterance
        // is the answer. "yes" / "yeah" / "do it" → re-run the original command.
        // Anything else → cancel.
        val pendingCmd = pendingConfirmationCommand
        if (pendingCmd != null) {
            pendingConfirmationCommand = null
            val lower = text.trim().lowercase()
            val confirmed = lower == "yes" || lower == "yeah" || lower == "do it" ||
                            lower == "confirm" || lower == "proceed" || lower == "ok" ||
                            lower == "okay" || lower.startsWith("yes ")
            if (confirmed) {
                Log.d(TAG, "Confirmation received — re-running: $pendingCmd")
                processVoiceInput(pendingCmd)
                return
            } else {
                val cancelMsg = "Action cancelled."
                persistKhwabMessage(cancelMsg)
                speak(cancelMsg)
                return
            }
        }

        // Persist the user's spoken query to Room so chat history is complete.
        val userMsgId = nextMsgId()
        try {
            chatMessageDao.upsert(
                ChatMessageEntity(
                    id = userMsgId,
                    text = text,
                    sender = Sender.USER.name,
                    timestamp = System.currentTimeMillis(),
                    status = MessageStatus.SENT.name,
                    state = MessageState.COMPLETE.name
                )
            )
        } catch (_: Exception) { /* non-fatal */ }

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

        // ── Safety confirmation gate ──────────────────────────────────────────
        if (response.requiresConfirmation) {
            val prompt = response.confirmationPrompt
                ?: "This action may be irreversible. Do you want me to continue?"
            pendingConfirmationCommand = text
            persistKhwabMessage(prompt)
            withContext(Dispatchers.Main) {
                floatingWindow.setState(AssistantState.SPEAKING)
                AssistantStateManager.updateState(AssistantState.SPEAKING)
            }
            speak(prompt)
            return
        }

        // ── Execute all plan steps in order with closed-loop screen re-read ───
        if (response.success) {
            val plans = response.executionPlans.ifEmpty {
                listOfNotNull(response.executionPlan)
            }

            if (plans.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    floatingWindow.setState(AssistantState.EXECUTING)
                    AssistantStateManager.updateState(AssistantState.EXECUTING)
                }

                val accessibilityActions = setOf(
                    "CLICK", "LONG_CLICK", "SCROLL", "SCROLL_TO_TOP", "SCROLL_TO_BOTTOM",
                    "SWIPE", "TYPE_TEXT", "GO_BACK", "GO_HOME",
                    "READ_SCREEN", "FIND_ELEMENT", "FOCUS_ELEMENT"
                )

                for (plan in plans) {
                    Log.d(TAG, "Executing step: ${plan.action}")

                    val isAccessibilityAction = plan.action in accessibilityActions
                    if (isAccessibilityAction && KhwabAccessibilityService.instance.get() == null) {
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
                    }

                    val success = executionEngine.execute(plan)
                    Log.d(TAG, "Step done: action=${plan.action} success=$success")

                    // Error recovery: speak feedback on failure but do not retry here —
                    // AccessibilityExecutor already does one retry internally.
                    if (!success && isAccessibilityAction) {
                        speak("Couldn't complete that step. Moving on.")
                    }

                    // Closed-loop: if this step might have changed the screen, wait for
                    // the new UI to settle then re-capture the accessibility snapshot.
                    // The fresh snapshot is logged but not re-sent to Core mid-plan
                    // (Core has already planned all steps); it is available for the
                    // next user command via AccessibilityTreeMapper.capture().
                    if (plan.requiresScreenRefresh) {
                        delay(SCREEN_SETTLE_MS)
                        val fresh = AccessibilityTreeMapper.capture()
                        if (fresh != null) {
                            Logger.info(
                                LogModule.ACCESSIBILITY,
                                "Screen refreshed after ${plan.action}: " +
                                "pkg=${fresh.packageName} elements=${fresh.allElements().size}"
                            )
                        }
                    } else {
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
            persistKhwabMessage(responseText)
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
            val geminiAnswer = fetchGeminiAnswer(query, text)
            if (!geminiAnswer.isNullOrBlank()) {
                withContext(Dispatchers.Main) {
                    floatingWindow.setState(AssistantState.SPEAKING)
                    AssistantStateManager.updateState(AssistantState.SPEAKING)
                }
                persistKhwabMessage(geminiAnswer)
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
     * Persists a Khwab reply to Room so the chat screen shows voice interactions.
     */
    private suspend fun persistKhwabMessage(text: String) {
        try {
            chatMessageDao.upsert(
                ChatMessageEntity(
                    id = nextMsgId(),
                    text = text,
                    sender = Sender.KHWAB.name,
                    timestamp = System.currentTimeMillis(),
                    status = MessageStatus.SENT.name,
                    state = MessageState.COMPLETE.name
                )
            )
        } catch (_: Exception) { /* non-fatal */ }
    }

    /**
     * Builds conversation history from Room (last 12 messages, both roles).
     * Mirrors the ChatViewModel approach so voice and chat share the same history.
     */
    private suspend fun buildRoomHistory(): List<Pair<String, String>> {
        return try {
            chatMessageDao.loadAll()
                .takeLast(12)
                .filter { it.state == MessageState.COMPLETE.name }
                .map { msg ->
                    val role = if (msg.sender == Sender.USER.name) "User" else "Khwab"
                    role to msg.text
                }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Calls Gemini directly, waits for the answer, speaks it, and caches it
     * in the temporary knowledge store (30-day TTL).
     *
     * Uses lazy singleton objects — no allocations on repeated calls.
     */
    private suspend fun fetchGeminiAnswer(query: String, originalText: String = query): String? {
        return try {
            val history = buildRoomHistory()
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

