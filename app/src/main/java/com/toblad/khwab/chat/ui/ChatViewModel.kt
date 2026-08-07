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

    private val _uiState = MutableStateFlow(
        ChatUiState(
            messages = listOf(
                ChatMessage(
                    id = nextId(),
                    text = "Hello Mr. Bhushan! I'm Khwab. How can I help you today?",
                    sender = Sender.KHWAB
                )
            )
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    /** Tracks background knowledge acquisition state for UI indicators. */
    private val _acquisitionState = MutableStateFlow<KnowledgeAcquisitionState>(
        KnowledgeAcquisitionState.Idle
    )
    val acquisitionState: StateFlow<KnowledgeAcquisitionState> =
        _acquisitionState.asStateFlow()

    /**
     * The ID of the placeholder "Let me look into that…" bubble that was inserted
     * when an acquisition was started. When Gemini returns, we replace this bubble
     * in-place instead of appending a second message.
     */
    private var pendingPlaceholderMessageId: Long? = null

    /**
     * The last answer surfaced to the user (from Gemini acquisition or temp memory recall).
     * Used so "remember this" can save it permanently regardless of which path answered.
     */
    private var lastAnswerForMemory: String? = null
    private var lastAnswerQuery: String? = null

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
            it.copy(
                input = "",
                isTyping = true,
                messages = it.messages + userMessage
            )
        }

        viewModelScope.launch {
            delay(300)

            val response = chatEngine.process(input)

            val replyText = if (response.success) {
                response.message ?: "I'm not sure how to respond."
            } else {
                response.error?.message ?: "Something went wrong."
            }

            if (response.requiresAcquisition) {
                // ── Gemini acquisition path ───────────────────────────────────
                // Insert a placeholder bubble that will be replaced in-place when
                // Gemini returns the real answer.
                val placeholderId = nextId()
                pendingPlaceholderMessageId = placeholderId

                _uiState.update { state ->
                    state.copy(
                        isTyping = false,
                        messages = state.messages + ChatMessage(
                            id = placeholderId,
                            text = replyText,   // "Let me look into that…"
                            sender = Sender.KHWAB,
                            status = MessageStatus.SENDING,   // spinner tint
                            state = MessageState.STREAMING    // cursor shows
                        )
                    )
                }

                val query = response.acquisitionQuery ?: input
                lastAnswerForMemory = null
                lastAnswerQuery = query
                _acquisitionState.value = KnowledgeAcquisitionState.Acquiring(query)
                val workId = KnowledgeAcquisitionWorker.enqueue(context, query)
                observeAcquisition(workId, query, placeholderId)

            } else {
                // ── Answered locally (memory / conversation / command) ────────
                val khwabMsg = ChatMessage(
                    id = nextId(),
                    text = replyText,
                    sender = Sender.KHWAB,
                    status = MessageStatus.SENT,
                    state = MessageState.COMPLETE
                )
                _uiState.update { state ->
                    state.copy(isTyping = false, messages = state.messages + khwabMsg)
                }

                // Track the local answer for "remember this" too
                // (e.g. Khwab answered from 30-day temp memory — user may want to pin it)
                if (response.success && !replyText.isNullOrBlank() &&
                    !replyText.startsWith("I don't have") &&
                    !replyText.startsWith("I'm not sure") &&
                    !replyText.startsWith("Could you clarify") &&
                    !replyText.startsWith("I can't") &&
                    !replyText.startsWith("I'm unable")) {
                    lastAnswerForMemory = replyText
                    lastAnswerQuery = input
                }
            }

            if (response.success) {
                response.executionPlan?.let { plan -> executionEngine.execute(plan) }

                response.forgetLearnedKey?.let { key ->
                    viewModelScope.launch {
                        val repo = RoomTemporaryKnowledgeRepository(
                            KhwabDatabase.getInstance(context).temporaryKnowledgeDao()
                        )
                        repo.deleteByKey(key)
                    }
                }
            }
        }
    }

    /**
     * Observes a [KnowledgeAcquisitionWorker] job.
     *
     * On SUCCEEDED: replaces the [placeholderId] bubble in-place with the real answer.
     *               No new bubble is appended — the existing one is updated.
     * On FAILED/CANCELLED: updates the placeholder bubble to show an error message.
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
                                // Replace the placeholder bubble in-place
                                _uiState.update { state ->
                                    state.copy(
                                        messages = state.messages.map { msg ->
                                            if (msg.id == placeholderId) {
                                                msg.copy(
                                                    text = answer,
                                                    status = MessageStatus.SENT,
                                                    state = MessageState.COMPLETE
                                                )
                                            } else msg
                                        }
                                    )
                                }
                            } else {
                                // Gemini returned nothing useful — update placeholder to say so
                                replacePlaceholder(
                                    placeholderId,
                                    "I couldn't find a good answer for \"$query\". Try rephrasing?"
                                )
                            }
                            pendingPlaceholderMessageId = null
                            _acquisitionState.value = KnowledgeAcquisitionState.Completed(query)
                        }
                        else -> {
                            replacePlaceholder(
                                placeholderId,
                                "Something went wrong while looking up \"$query\". Please try again."
                            )
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

    /** Updates the text and state of a bubble identified by [id]. */
    private fun replacePlaceholder(id: Long, text: String) {
        _uiState.update { state ->
            state.copy(
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

    /**
     * Saves [lastAnswerForMemory] into permanent Room memory.
     * Works regardless of whether the answer came from Gemini or local temp memory.
     * The creation timestamp is stored inside Memory.metadata automatically.
     * When the user later asks "when did I save this?" / "recall X", CoreStepExecutor
     * surfaces the saved date.
     */
    private fun saveLastAnswerPermanently() {
        val answer = lastAnswerForMemory ?: return
        val query = lastAnswerQuery ?: "learned answer"

        viewModelScope.launch {
            val db = KhwabDatabase.getInstance(context)
            val permanentMemory = RoomPermanentMemory(db.permanentMemoryDao())

            permanentMemory.create(
                Memory.createPermanent(
                    subject = query.trim().lowercase(),
                    value = answer,
                    category = MemoryCategory.PREFERENCE,
                    confidence = MemoryConfidence.EXPLICIT
                )
            )

            lastAnswerForMemory = null
            lastAnswerQuery = null

            val confirmMsg = "🔒 Saved permanently! I'll always remember this answer about \"$query\".\n" +
                             "   Say **\"recall $query\"** anytime to get it back, or ask me when you saved it."
            _uiState.update { state ->
                state.copy(
                    messages = state.messages + ChatMessage(
                        id = nextId(),
                        text = confirmMsg,
                        sender = Sender.KHWAB,
                        status = MessageStatus.SENT,
                        state = MessageState.COMPLETE
                    )
                )
            }
        }
    }
}
