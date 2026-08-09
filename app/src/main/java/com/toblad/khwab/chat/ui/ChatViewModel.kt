package com.toblad.khwab.chat.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.toblad.khwab.background.KnowledgeAcquisitionState
import com.toblad.khwab.background.KnowledgeAcquisitionWorker
import com.toblad.khwab.chat.engine.ChatEngine
import com.toblad.khwab.chat.model.ChatMessage
import com.toblad.khwab.chat.model.MessageState
import com.toblad.khwab.chat.model.MessageStatus
import com.toblad.khwab.chat.model.Sender
import com.toblad.khwab.core.memory.model.Memory
import com.toblad.khwab.core.memory.model.MemoryCategory
import com.toblad.khwab.core.memory.model.MemoryConfidence
import com.toblad.khwab.db.KhwabDatabase
import com.toblad.khwab.db.dao.ChatMessageDao
import com.toblad.khwab.db.entity.ChatMessageEntity
import com.toblad.khwab.db.repository.RoomPermanentMemory
import com.toblad.khwab.db.repository.RoomTemporaryKnowledgeRepository
import com.toblad.khwab.di.KhwabProvider
import com.toblad.khwab.executor.AndroidExecutionEngine
import com.toblad.khwab.integration.model.execution.ExecutionPlan
import com.toblad.khwab.integration.model.task.TaskState
import com.toblad.khwab.service.AccessibilityTreeMapper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

class ChatViewModel(
    application: Application
) : AndroidViewModel(application) {

    companion object {
        /** Delay between plan steps that do not change the screen. */
        private const val STEP_DELAY_MS = 600L
        /** Delay after a step that triggers a screen transition. */
        private const val SCREEN_SETTLE_MS = 1200L
    }

    private val idCounter = AtomicLong(System.currentTimeMillis())
    private fun nextId() = idCounter.incrementAndGet()

    private val context = application.applicationContext

    init {
        KhwabProvider.init(context)
        android.util.Log.d("ChatViewModel", "ChatViewModel created successfully")
    }

    private val chatEngine: ChatEngine = KhwabProvider.chatEngine
    private val executionEngine = AndroidExecutionEngine(context)
    private val chatMessageDao: ChatMessageDao =
        KhwabDatabase.getInstance(context).chatMessageDao()

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    /** Tracks background knowledge acquisition state for the "Learning…" strip. */
    private val _acquisitionState = MutableStateFlow<KnowledgeAcquisitionState>(
        KnowledgeAcquisitionState.Idle
    )
    val acquisitionState: StateFlow<KnowledgeAcquisitionState> =
        _acquisitionState.asStateFlow()

    /** ID of the placeholder bubble inserted when Gemini acquisition starts. */
    private var pendingPlaceholderMessageId: Long? = null

    /**
     * Tracks the coroutine running [runDynamicExecutionLoop] so it can be
     * cancelled when the user leaves ChatActivity (e.g. an app was opened).
     * Without this, the replan loop keeps executing in the background and
     * re-issues OPEN_APP every time the user returns.
     */
    private var executionJob: Job? = null

    /**
     * Last answer shown to the user (Gemini or local memory).
     * Used so "remember this" can save it permanently.
     */
    private var lastAnswerForMemory: String? = null
    private var lastAnswerQuery: String? = null

    init {
        // Observe Room messages reactively — this handles both initial load and
        // live updates from VoiceService writing turns while chat is open.
        viewModelScope.launch {
            chatMessageDao.observeAll().collect { entities ->
                val messages = entities.map { it.toChatMessage() }
                if (messages.isEmpty()) {
                    // Seed the welcome message on first launch
                    val welcome = ChatMessage(
                        id = nextId(),
                        text = "Hello Mr. Bhushan! I'm Khwab. How can I help you today?",
                        sender = Sender.KHWAB
                    )
                    chatMessageDao.upsert(welcome.toEntity())
                    // Room Flow will emit again with the seeded message — no manual update needed
                } else {
                    // Merge: keep isNew/isTyping state for in-flight messages already in UI
                    val inFlight = _uiState.value.messages
                        .filter { it.isNew }
                        .associateBy { it.id }
                    val merged = messages.map { msg ->
                        inFlight[msg.id]?.let { live -> msg.copy(isNew = live.isNew) } ?: msg
                    }
                    _uiState.update { state ->
                        // Don't overwrite isTyping — it is managed by sendMessage/observeAcquisition
                        state.copy(messages = merged)
                    }
                }
            }
        }
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(input = text) }
    }

    /**
     * When a destructive confirmation is pending, holds the original command.
     * The next message is treated as the confirmation answer.
     */
    private var pendingConfirmationInput: String? = null

    fun sendMessage() {
        val input = uiState.value.input.trim()
        if (input.isBlank()) return

        // ── Confirmation reply handling ───────────────────────────────────────
        val pendingCmd = pendingConfirmationInput
        if (pendingCmd != null) {
            pendingConfirmationInput = null
            _uiState.update { it.copy(input = "") }
            val lower = input.lowercase()
            val confirmed = lower == "yes" || lower == "yeah" || lower == "do it" ||
                lower == "confirm" || lower == "proceed" || lower == "ok" ||
                lower == "okay" || lower.startsWith("yes ")
            if (confirmed) {
                // Re-submit the original command as if it were typed freshly.
                _uiState.update { it.copy(input = pendingCmd) }
                sendMessage()
            } else {
                appendKhwabMessage("Action cancelled.")
            }
            return
        }

        // Short-circuit: "remember this" saves the last answer permanently
        if (isRememberThisCommand(input) && lastAnswerForMemory != null) {
            _uiState.update { it.copy(input = "") }
            saveLastAnswerPermanently()
            return
        }

        val userMessage = ChatMessage(
            id = nextId(),
            text = input,
            sender = Sender.USER,
            status = MessageStatus.SENT,
            state = MessageState.COMPLETE
        )

        _uiState.update {
            it.copy(input = "", isTyping = true, messages = it.messages + userMessage)
        }

        // Persist user message immediately
        viewModelScope.launch { chatMessageDao.upsert(userMessage.toEntity()) }

        viewModelScope.launch {
            delay(300)

            val response = chatEngine.process(input)

            // ── Safety confirmation gate ──────────────────────────────────────
            if (response.requiresConfirmation) {
                val prompt = response.confirmationPrompt
                    ?: "This action may be irreversible. Do you want me to continue?"
                pendingConfirmationInput = input
                appendKhwabMessage(prompt)
                _uiState.update { it.copy(isTyping = false) }
                return@launch
            }

            val replyText = if (response.success) {
                response.message ?: "I'm not sure how to respond."
            } else {
                response.error?.message ?: "Something went wrong."
            }

            if (response.requiresAcquisition) {
                // ── Gemini path: insert placeholder, keep isTyping = true ─────
                val placeholderId = nextId()
                pendingPlaceholderMessageId = placeholderId

                val placeholderMsg = ChatMessage(
                    id = placeholderId,
                    text = replyText,   // "Let me look into that…"
                    sender = Sender.KHWAB,
                    status = MessageStatus.SENDING,
                    state = MessageState.STREAMING
                )

                // Persist placeholder — will be updated when Gemini answers
                chatMessageDao.upsert(placeholderMsg.toEntity())

                _uiState.update { state ->
                    state.copy(
                        isTyping = true,    // keep typing indicator while waiting
                        messages = state.messages + placeholderMsg
                    )
                }

                val query = response.acquisitionQuery ?: input
                lastAnswerForMemory = null
                lastAnswerQuery = query
                _acquisitionState.value = KnowledgeAcquisitionState.Acquiring(query)

                // Build conversation history to send with the prompt
                val history = buildConversationHistory()
                val workId = KnowledgeAcquisitionWorker.enqueue(context, query, history = history)
                observeAcquisition(workId, query, placeholderId)

            } else {
                // ── Local answer (memory / conversation / command) ────────────
                val khwabMsg = ChatMessage(
                    id = nextId(),
                    text = replyText,
                    sender = Sender.KHWAB,
                    status = MessageStatus.SENT,
                    state = MessageState.COMPLETE
                )
                chatMessageDao.upsert(khwabMsg.toEntity())
                _uiState.update { state ->
                    state.copy(isTyping = false, messages = state.messages + khwabMsg)
                }

                // Track for "remember this"
                if (response.success && isAnswerWorthRemembering(replyText)) {
                    lastAnswerForMemory = replyText
                    lastAnswerQuery = input
                }
            }

            if (response.success) {
                // ── Dynamic execution loop: Observe → Reason → Act ───────────
                val initialPlans = response.executionPlans.ifEmpty {
                    listOfNotNull(response.executionPlan)
                }
                if (initialPlans.isNotEmpty()) {
                    // Cancel any prior loop before starting a new one, then
                    // track the job so cancelExecution() can stop it if the
                    // user leaves the Activity while the loop is running.
                    executionJob?.cancel()
                    executionJob = viewModelScope.launch {
                        runDynamicExecutionLoop(
                            originalGoal = input,
                            initialPlans = initialPlans
                        )
                    }
                }

                response.forgetLearnedKey?.let { key ->
                    viewModelScope.launch {
                        RoomTemporaryKnowledgeRepository(
                            KhwabDatabase.getInstance(context).temporaryKnowledgeDao()
                        ).deleteByKey(key)
                    }
                }
            }
        }
    }

    // ── Dynamic execution loop ────────────────────────────────────────────────

    /**
     * Runs the Observe → Reason → Act loop for multi-step screen-aware execution.
     *
     * Mirrors [VoiceService.runDynamicExecutionLoop] so Voice and Chat share
     * the same dynamic behaviour.  The only difference is that Chat appends
     * status messages as chat bubbles rather than speaking them.
     */
    private suspend fun runDynamicExecutionLoop(
        originalGoal: String,
        initialPlans: List<ExecutionPlan>
    ) {
        val completedActions = mutableListOf<String>()
        var totalStepsExecuted = 0
        var retryCount = 0
        var switchedToDynamic = false

        for (plan in initialPlans) {
            android.util.Log.d("ChatViewModel", "[DynLoop] Initial step: ${plan.action}")

            val success = try { executionEngine.execute(plan) } catch (_: Exception) { false }
            totalStepsExecuted++

            val actionDesc = buildActionDesc(plan)

            if (plan.requiresScreenRefresh) {
                delay(SCREEN_SETTLE_MS)
                completedActions.add(actionDesc)

                val freshScreen = AccessibilityTreeMapper.capture()

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

                switchedToDynamic = true
                while (true) {
                    if (taskState.isOverLimit()) {
                        android.util.Log.w("ChatViewModel", "[DynLoop] Safety cap reached")
                        break
                    }

                    android.util.Log.d("ChatViewModel",
                        "[DynLoop] Replanning. Completed: ${taskState.completedActions}")

                    val replanResult = try {
                        chatEngine.replan(taskState)
                    } catch (e: Exception) {
                        android.util.Log.e("ChatViewModel", "[DynLoop] Replan exception", e)
                        break
                    }

                    when {
                        replanResult.isComplete -> {
                            android.util.Log.d("ChatViewModel", "[DynLoop] Task complete")
                            val msg = replanResult.statusMessage
                            if (!msg.isNullOrBlank()) {
                                appendKhwabMessage(msg)
                            }
                            break
                        }

                        replanResult.isFailed -> {
                            val msg = replanResult.statusMessage ?: "I couldn't complete that task."
                            appendKhwabMessage(msg)
                            break
                        }

                        replanResult.requiresConfirmation -> {
                            val prompt = replanResult.confirmationPrompt
                                ?: "This action may be irreversible. Do you want me to continue?"
                            pendingConfirmationInput = originalGoal
                            appendKhwabMessage(prompt)
                            break
                        }

                        replanResult.nextStep != null -> {
                            val nextPlan = replanResult.nextStep!!
                            android.util.Log.d("ChatViewModel",
                                "[DynLoop] Next step: ${nextPlan.action}")

                            val stepSuccess = try {
                                executionEngine.execute(nextPlan)
                            } catch (_: Exception) { false }
                            totalStepsExecuted++

                            val nextActionDesc = replanResult.actionDescription
                                ?: buildActionDesc(nextPlan)

                            if (nextPlan.requiresScreenRefresh) {
                                delay(SCREEN_SETTLE_MS)

                                val nextScreen = AccessibilityTreeMapper.capture()
                                val newRetry = if (stepSuccess) 0 else retryCount + 1
                                retryCount = newRetry

                                if (stepSuccess) completedActions.add(nextActionDesc)

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

                            } else {
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
                            android.util.Log.d("ChatViewModel",
                                "[DynLoop] No next step — treating as complete")
                            break
                        }
                    }
                }

                break
            } else {
                completedActions.add(actionDesc)
                delay(STEP_DELAY_MS)
            }
        }

        if (!switchedToDynamic) {
            android.util.Log.d("ChatViewModel",
                "[DynLoop] Static plan completed without dynamic replanning")
        }
    }

    private fun buildActionDesc(plan: ExecutionPlan): String {
        val name = plan.action.lowercase().replace('_', ' ')
        return if (!plan.target.isNullOrBlank()) "$name '${plan.target}'" else name
    }

    // ─────────────────────────────────────────────────────────────────────────

    /** Appends a Khwab reply bubble to both Room and in-memory UI state. */
    private fun appendKhwabMessage(text: String) {
        val msg = ChatMessage(
            id = nextId(),
            text = text,
            sender = Sender.KHWAB,
            status = MessageStatus.SENT,
            state = MessageState.COMPLETE
        )
        viewModelScope.launch { chatMessageDao.upsert(msg.toEntity()) }
        _uiState.update { state -> state.copy(messages = state.messages + msg) }
    }

    /**
     * Builds the last 6 turns as a list of (role, text) pairs for Gemini context.
     * Uses the current in-memory message list — already up to date.
     */
    private fun buildConversationHistory(): List<Pair<String, String>> {
        return _uiState.value.messages
            .takeLast(12)   // up to 6 user + 6 khwab turns
            .filter { it.state == MessageState.COMPLETE }
            .map { msg ->
                val role = if (msg.sender == Sender.USER) "User" else "Khwab"
                role to msg.text
            }
    }

    /**
     * Observes a [KnowledgeAcquisitionWorker] job.
     * On SUCCEEDED — replaces the placeholder bubble in-place, hides typing indicator.
     * On failure — shows an error message in the placeholder bubble.
     */
    private fun observeAcquisition(workId: UUID, query: String, placeholderId: Long) {
        viewModelScope.launch {
            WorkManager.getInstance(context)
                .getWorkInfoByIdFlow(workId)
                .first { info -> info != null && info.state.isFinished }
                ?.let { info ->
                    when (info.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            val answer = info.outputData
                                .getString(KnowledgeAcquisitionWorker.KEY_ANSWER)

                            if (!answer.isNullOrBlank()) {
                                lastAnswerForMemory = answer

                                // Update Room first, then UI
                                chatMessageDao.updateContent(
                                    id = placeholderId,
                                    text = answer,
                                    status = "SENT",
                                    state = "COMPLETE"
                                )

                                _uiState.update { state ->
                                    state.copy(
                                        isTyping = false,
                                        messages = state.messages.map { msg ->
                                            if (msg.id == placeholderId)
                                                msg.copy(
                                                    text = answer,
                                                    status = MessageStatus.SENT,
                                                    state = MessageState.COMPLETE,
                                                    isNew = true  // typewriter for Gemini answers too
                                                )
                                            else msg
                                        }
                                    )
                                }
                            } else {
                                val errorText =
                                    "I couldn't find a good answer for \"$query\". Try rephrasing?"
                                replacePlaceholder(placeholderId, errorText)
                            }
                            pendingPlaceholderMessageId = null
                            _acquisitionState.value = KnowledgeAcquisitionState.Completed(query)
                        }
                        else -> {
                            val errorText =
                                "Something went wrong while looking up \"$query\". Please try again."
                            replacePlaceholder(placeholderId, errorText)
                            pendingPlaceholderMessageId = null
                            _acquisitionState.value = KnowledgeAcquisitionState.Failed(
                                query = query,
                                reason = "Knowledge acquisition did not complete."
                            )
                        }
                    }
                }
        }
    }

    /** Updates a bubble in both Room and the in-memory UI state. */
    private fun replacePlaceholder(id: Long, text: String) {
        viewModelScope.launch {
            chatMessageDao.updateContent(id, text, "SENT", "COMPLETE")
        }
        _uiState.update { state ->
            state.copy(
                isTyping = false,
                messages = state.messages.map { msg ->
                    if (msg.id == id) msg.copy(
                        text = text,
                        status = MessageStatus.SENT,
                        state = MessageState.COMPLETE
                    ) else msg
                }
            )
        }
    }

    // ── Typewriter completion ─────────────────────────────────────────────────

    /**
     * Called by [ChatBubble] once the typewriter animation finishes.
     * Clears the [ChatMessage.isNew] flag so the message renders with full
     * markdown on recomposition (e.g. after a theme change or scroll back).
     */
    fun onTypewriterFinished(messageId: Long) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.map { msg ->
                    if (msg.id == messageId && msg.isNew) msg.copy(isNew = false)
                    else msg
                }
            )
        }
    }

    // ── Clear chat ────────────────────────────────────────────────────────────

    fun clearChat() {
        viewModelScope.launch {
            chatMessageDao.deleteAll()
            val welcome = ChatMessage(
                id = nextId(),
                text = "Conversation cleared. How can I help you?",
                sender = Sender.KHWAB,
                status = MessageStatus.SENT,
                state = MessageState.COMPLETE
            )
            chatMessageDao.upsert(welcome.toEntity())
            _uiState.update { ChatUiState(messages = listOf(welcome)) }
            lastAnswerForMemory = null
            lastAnswerQuery = null
        }
    }

    // ── Execution control ─────────────────────────────────────────────────────

    /**
     * Cancels any in-flight dynamic execution loop.
     * Called by [ChatActivity.onStop] so that leaving the Activity while an
     * OPEN_APP (or any other action) is executing does not cause it to
     * re-run when the user comes back.
     */
    fun cancelExecution() {
        executionJob?.cancel()
        executionJob = null
    }

    // ── "Remember this" helpers ───────────────────────────────────────────────

    private fun isRememberThisCommand(input: String): Boolean {
        val lower = input.trim().lowercase()
        return lower == "remember this" ||
               lower == "remember this answer" ||
               lower == "save this" ||
               lower == "save this answer" ||
               lower == "please remember this" ||
               lower == "yes remember this" ||
               lower == "store this" ||
               lower.startsWith("remember this") ||
               lower.startsWith("please remember this") ||
               lower.startsWith("save this answer") ||
               lower.startsWith("store this")
    }

    private fun isAnswerWorthRemembering(text: String): Boolean {
        val lower = text.lowercase()
        return !lower.startsWith("i don't have") &&
               !lower.startsWith("i'm not sure") &&
               !lower.startsWith("could you clarify") &&
               !lower.startsWith("i can't") &&
               !lower.startsWith("i'm unable") &&
               !lower.startsWith("something went wrong") &&
               !lower.startsWith("i couldn't find")
    }

    /**
     * Saves [lastAnswerForMemory] permanently in Room.
     * Creation timestamp is inside Memory.metadata — surfaced by CoreStepExecutor.RECALL.
     */
    private fun saveLastAnswerPermanently() {
        val answer = lastAnswerForMemory ?: return
        val query = lastAnswerQuery ?: "learned answer"

        viewModelScope.launch {
            RoomPermanentMemory(
                KhwabDatabase.getInstance(context).permanentMemoryDao()
            ).create(
                Memory.createPermanent(
                    subject = query.trim().lowercase(),
                    value = answer,
                    category = MemoryCategory.PREFERENCE,
                    confidence = MemoryConfidence.EXPLICIT
                )
            )

            lastAnswerForMemory = null
            lastAnswerQuery = null

            val confirmMsg =
                "🔒 Saved permanently! I'll always remember this answer about \"$query\".\n" +
                "   Say **\"recall $query\"** anytime to get it back."
            val msg = ChatMessage(
                id = nextId(),
                text = confirmMsg,
                sender = Sender.KHWAB,
                status = MessageStatus.SENT,
                state = MessageState.COMPLETE
            )
            chatMessageDao.upsert(msg.toEntity())
            _uiState.update { state ->
                state.copy(messages = state.messages + msg)
            }
        }
    }

    // ── Entity mapping helpers ────────────────────────────────────────────────

    private fun ChatMessage.toEntity() = ChatMessageEntity(
        id = id,
        text = text,
        sender = sender.name,
        timestamp = timestamp,
        status = status.name,
        state = state.name
    )

    private fun ChatMessageEntity.toChatMessage() = ChatMessage(
        id = id,
        text = text,
        sender = Sender.valueOf(sender),
        timestamp = timestamp,
        status = MessageStatus.valueOf(status),
        state = MessageState.valueOf(state)
    )
}
