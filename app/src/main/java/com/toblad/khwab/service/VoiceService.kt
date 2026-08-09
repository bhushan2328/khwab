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
import com.toblad.khwab.permission.AccessibilityPermissionHelper
import com.toblad.khwab.integration.llm.LLMService
import com.toblad.khwab.integration.llm.providers.LLMKnowledgeExtractor
import com.toblad.khwab.integration.llm.providers.RelatedPromptBuilder
import com.toblad.khwab.integration.model.execution.ExecutionPlan
import com.toblad.khwab.integration.model.task.TaskState
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

        // ── Dynamic execution loop: Observe → Reason → Act ───────────────────
        if (response.success) {
            val initialPlans = response.executionPlans.ifEmpty {
                listOfNotNull(response.executionPlan)
            }

            if (initialPlans.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    floatingWindow.setState(AssistantState.EXECUTING)
                    AssistantStateManager.updateState(AssistantState.EXECUTING)
                }

                runDynamicExecutionLoop(
                    originalGoal = text,
                    initialPlans = initialPlans
                )
            }
        }

        // ── READ_SCREEN: speak captured text ──────────────────────────────────
        // Read the service reference once so both the read and the clear operate
        // on the same object (avoids a TOCTOU if the service is replaced between calls).
        val screenReadText = KhwabAccessibilityService.instance.get()?.let { svc ->
            val text = svc.lastScreenReadResult
            if (text != null) svc.lastScreenReadResult = null
            text
        }

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

    // ── Dynamic execution loop ────────────────────────────────────────────────

    /**
     * Runs the Observe → Reason → Act loop for multi-step screen-aware execution.
     *
     * Strategy:
     *  1. Execute the first step from the initial plan.
     *  2. If it requires a screen refresh: wait, capture fresh screen, call replan().
     *  3. If replan() returns another step: execute it and repeat.
     *  4. If replan() returns isComplete: speak the completion message, stop.
     *  5. If replan() returns isFailed: speak the failure, stop.
     *  6. If replan() returns requiresConfirmation: ask user, stop (user reply
     *     will retrigger via pendingConfirmationCommand).
     *  7. Steps that do NOT change the screen proceed directly to the next
     *     step in the initial plan (no replan needed — screen hasn't changed).
     */
    private suspend fun runDynamicExecutionLoop(
        originalGoal: String,
        initialPlans: List<ExecutionPlan>
    ) {
        val accessibilityActions = setOf(
            "CLICK", "LONG_CLICK", "SCROLL", "SCROLL_TO_TOP", "SCROLL_TO_BOTTOM",
            "SWIPE", "TYPE_TEXT", "GO_BACK", "GO_HOME",
            "READ_SCREEN", "FIND_ELEMENT", "FOCUS_ELEMENT"
        )

        // Check accessibility availability once before the loop.
        // Use isEnabledBySystem (system registry check) rather than the raw instance
        // reference — the instance may still be null in the brief window between the
        // user granting the permission and onServiceConnected() firing, which would
        // incorrectly send the user back to Settings even though permission is granted.
        if (initialPlans.any { it.action in accessibilityActions } &&
            !AccessibilityPermissionHelper.isEnabledBySystem(this)) {
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
            return
        }

        // State tracking for the dynamic loop.
        val completedActions = mutableListOf<String>()
        var totalStepsExecuted = 0
        var retryCount = 0

        // Phase 1: execute the initial plan steps sequentially.
        // For steps that change the screen, we break and switch to dynamic replanning.
        var switchedToDynamic = false

        for (plan in initialPlans) {
            Log.d(TAG, "[DynLoop] Initial step: ${plan.action}")

            val success = executionEngine.execute(plan)
            totalStepsExecuted++
            Log.d(TAG, "[DynLoop] Step done: action=${plan.action} success=$success")

            val actionDesc = buildActionDesc(plan)

            if (plan.requiresScreenRefresh) {
                // Screen likely changed. Wait for it to settle, then switch to
                // dynamic replanning — the rest of the initial plan may be stale.
                delay(SCREEN_SETTLE_MS)
                completedActions.add(actionDesc)

                val freshScreen = AccessibilityTreeMapper.capture()
                if (freshScreen != null) {
                    Logger.info(
                        LogModule.ACCESSIBILITY,
                        "[DynLoop] Fresh screen after ${plan.action}: " +
                        "pkg=${freshScreen.packageName} elements=${freshScreen.allElements().size}"
                    )
                }

                // Build initial task state for the replan loop.
                var taskState = TaskState(
                    originalGoal = originalGoal,
                    completedActions = completedActions.toList(),
                    lastAction = plan.action,
                    lastActionSucceeded = success,
                    currentScreen = freshScreen,
                    currentPackage = freshScreen?.packageName,
                    currentWindowTitle = freshScreen?.windowTitle,
                    retryCount = 0,
                    totalStepsExecuted = totalStepsExecuted
                )

                // Phase 2: dynamic replan loop.
                switchedToDynamic = true
                while (true) {
                    if (taskState.isOverLimit()) {
                        Log.w(TAG, "[DynLoop] Safety cap reached — stopping")
                        speak("I've taken too many steps. Please try a simpler command.")
                        break
                    }

                    Log.d(TAG, "[DynLoop] Replanning. Completed: ${taskState.completedActions}")
                    val replanResult = try {
                        integration.replan(taskState)
                    } catch (e: Exception) {
                        Log.e(TAG, "[DynLoop] Replan exception", e)
                        break
                    }

                    when {
                        replanResult.isComplete -> {
                            Log.d(TAG, "[DynLoop] Task complete")
                            val msg = replanResult.statusMessage
                            if (!msg.isNullOrBlank()) {
                                persistKhwabMessage(msg)
                                withContext(Dispatchers.Main) {
                                    floatingWindow.setState(AssistantState.SPEAKING)
                                    AssistantStateManager.updateState(AssistantState.SPEAKING)
                                }
                                speak(msg)
                            }
                            break
                        }

                        replanResult.isFailed -> {
                            Log.w(TAG, "[DynLoop] Task failed: ${replanResult.statusMessage}")
                            val msg = replanResult.statusMessage ?: "I couldn't complete that task."
                            persistKhwabMessage(msg)
                            withContext(Dispatchers.Main) {
                                floatingWindow.setState(AssistantState.SPEAKING)
                                AssistantStateManager.updateState(AssistantState.SPEAKING)
                            }
                            speak(msg)
                            break
                        }

                        replanResult.requiresConfirmation -> {
                            val prompt = replanResult.confirmationPrompt
                                ?: "This action may be irreversible. Do you want me to continue?"
                            // Store the original goal as the pending command so user's
                            // "yes" re-runs the whole task from the current state.
                            pendingConfirmationCommand = originalGoal
                            persistKhwabMessage(prompt)
                            withContext(Dispatchers.Main) {
                                floatingWindow.setState(AssistantState.SPEAKING)
                                AssistantStateManager.updateState(AssistantState.SPEAKING)
                            }
                            speak(prompt)
                            break
                        }

                        replanResult.nextStep != null -> {
                            val nextPlan = replanResult.nextStep!!
                            Log.d(TAG, "[DynLoop] Next step: ${nextPlan.action}")

                            val stepSuccess = executionEngine.execute(nextPlan)
                            totalStepsExecuted++
                            Log.d(TAG, "[DynLoop] Next step done: action=${nextPlan.action} success=$stepSuccess")

                            val nextActionDesc = replanResult.actionDescription
                                ?: buildActionDesc(nextPlan)

                            if (nextPlan.requiresScreenRefresh) {
                                delay(SCREEN_SETTLE_MS)

                                val nextScreen = AccessibilityTreeMapper.capture()
                                if (nextScreen != null) {
                                    Logger.info(
                                        LogModule.ACCESSIBILITY,
                                        "[DynLoop] Screen after ${nextPlan.action}: " +
                                        "pkg=${nextScreen.packageName} " +
                                        "elements=${nextScreen.allElements().size}"
                                    )
                                }

                                // Update retry count: reset on success, increment on failure.
                                val newRetry = if (stepSuccess) 0 else retryCount + 1
                                retryCount = newRetry

                                if (stepSuccess) {
                                    completedActions.add(nextActionDesc)
                                }

                                taskState = TaskState(
                                    originalGoal = originalGoal,
                                    completedActions = completedActions.toList(),
                                    lastAction = nextPlan.action,
                                    lastActionSucceeded = stepSuccess,
                                    currentScreen = nextScreen,
                                    currentPackage = nextScreen?.packageName,
                                    currentWindowTitle = nextScreen?.windowTitle,
                                    retryCount = newRetry,
                                    totalStepsExecuted = totalStepsExecuted
                                )
                                // Continue loop — will replan with new screen.

                            } else {
                                // Step doesn't change screen — no replan needed yet.
                                // Add to completed and continue the replan loop with
                                // same screen (Core will pick the next action).
                                delay(STEP_DELAY_MS)
                                if (stepSuccess) completedActions.add(nextActionDesc)

                                taskState = taskState.copy(
                                    completedActions = completedActions.toList(),
                                    lastAction = nextPlan.action,
                                    lastActionSucceeded = stepSuccess,
                                    retryCount = if (stepSuccess) 0 else retryCount + 1,
                                    totalStepsExecuted = totalStepsExecuted
                                )
                            }
                        }

                        else -> {
                            // replanResult has no nextStep and is not complete/failed —
                            // treat as completion (Core had nothing more to plan).
                            Log.d(TAG, "[DynLoop] No next step — treating as complete")
                            break
                        }
                    }
                }

                // After switching to dynamic, skip remaining initial plan steps.
                break
            } else {
                // Step doesn't require screen refresh — execute directly and continue.
                completedActions.add(actionDesc)
                delay(STEP_DELAY_MS)
            }
        }

        // If we never switched to dynamic (all steps were non-screen-changing),
        // nothing more to do — initial plan completed.
        if (!switchedToDynamic) {
            Log.d(TAG, "[DynLoop] Static plan completed without dynamic replanning")
        }
    }

    /** Builds a short human-readable description of an [ExecutionPlan] for logging. */
    private fun buildActionDesc(plan: ExecutionPlan): String {
        val name = plan.action.lowercase().replace('_', ' ')
        return if (!plan.target.isNullOrBlank()) "$name '${plan.target}'" else name
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

