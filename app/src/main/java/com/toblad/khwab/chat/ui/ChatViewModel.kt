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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

class ChatViewModel(
    application: Application
) : AndroidViewModel(application) {

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
     * Last answer shown to the user (Gemini or local memory).
     * Used so "remember this" can save it permanently.
     */
    private var lastAnswerForMemory: String? = null
    private var lastAnswerQuery: String? = null

    init {
        // Load persisted messages from Room on startup
        viewModelScope.launch {
            val saved = chatMessageDao.loadAll().map { it.toChatMessage() }
            val messages = if (saved.isEmpty()) {
                listOf(
                    ChatMessage(
                        id = nextId(),
                        text = "Hello Mr. Bhushan! I'm Khwab. How can I help you today?",
                        sender = Sender.KHWAB
                    ).also { msg ->
                        chatMessageDao.upsert(msg.toEntity())
                    }
                )
            } else saved
            _uiState.update { it.copy(messages = messages) }
        }
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(input = text) }
    }

    fun sendMessage() {
        val input = uiState.value.input.trim()
        if (input.isBlank()) return

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
                response.executionPlan?.let { plan -> executionEngine.execute(plan) }

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
                                                    state = MessageState.COMPLETE
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

    // ── "Remember this" helpers ───────────────────────────────────────────────

    private fun isRememberThisCommand(input: String): Boolean {
        val lower = input.trim().lowercase()
        return lower == "remember this" ||
               lower == "remember this answer" ||
               lower == "save this" ||
               lower == "save this answer" ||
               lower.startsWith("remember this") ||
               lower.startsWith("please remember this")
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
