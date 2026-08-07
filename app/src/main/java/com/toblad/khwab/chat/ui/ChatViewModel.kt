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
import com.toblad.khwab.core.memory.model.MemoryCategory
import com.toblad.khwab.core.memory.model.MemoryConfidence
import com.toblad.khwab.core.memory.model.Memory
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
        // Ensure KhwabProvider is initialised with context (idempotent)
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
     * Holds the last AI-fetched answer so "remember this" can save it permanently.
     * Cleared whenever a new acquisition starts.
     */
    private var lastAcquisitionAnswer: String? = null
    private var lastAcquisitionQuery: String? = null

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(input = text) }
    }

    fun sendMessage() {
        val input = uiState.value.input.trim()
        if (input.isBlank()) return

        // Short-circuit: "remember this" on last AI answer → save permanently
        if (isRememberThisCommand(input) && lastAcquisitionAnswer != null) {
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

            _uiState.update { state ->
                state.copy(
                    isTyping = false,
                    messages = state.messages + ChatMessage(
                        id = nextId(),
                        text = replyText,
                        sender = Sender.KHWAB,
                        status = MessageStatus.SENT,
                        state = MessageState.COMPLETE
                    )
                )
            }

            if (response.success) {
                // Execute Android-side plan (open app, call, etc.)
                response.executionPlan?.let { plan ->
                    executionEngine.execute(plan)
                }

                // Schedule background knowledge acquisition if needed
                if (response.requiresAcquisition) {
                    val query = response.acquisitionQuery ?: input
                    lastAcquisitionAnswer = null
                    lastAcquisitionQuery = query
                    _acquisitionState.value = KnowledgeAcquisitionState.Acquiring(query)
                    val workId = KnowledgeAcquisitionWorker.enqueue(context, query)
                    observeAcquisition(workId, query)
                }

                // If user asked to forget learned knowledge, delete from temp store
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
     * Observes a [KnowledgeAcquisitionWorker] job by its [workId].
     *
     * On SUCCEEDED → stores the answer in [lastAcquisitionAnswer] and posts it
     * as a Khwab message so the user can reply "remember this" to save it permanently.
     */
    private fun observeAcquisition(workId: UUID, query: String) {
        viewModelScope.launch {
            WorkManager.getInstance(context)
                .getWorkInfoByIdFlow(workId)
                .first { info ->
                    info != null && info.state.isFinished
                }
                ?.let { info ->
                    when (info.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            val answer = info.outputData
                                .getString(KnowledgeAcquisitionWorker.KEY_ANSWER)
                            if (!answer.isNullOrBlank()) {
                                lastAcquisitionAnswer = answer
                                _uiState.update { state ->
                                    state.copy(
                                        messages = state.messages + ChatMessage(
                                            id = nextId(),
                                            text = answer,
                                            sender = Sender.KHWAB,
                                            status = MessageStatus.SENT,
                                            state = MessageState.COMPLETE
                                        )
                                    )
                                }
                            }
                            _acquisitionState.value =
                                KnowledgeAcquisitionState.Completed(query)
                        }
                        else -> {
                            _acquisitionState.value = KnowledgeAcquisitionState.Failed(
                                query = query,
                                reason = "Knowledge acquisition did not complete."
                            )
                        }
                    }
                }
        }
    }

    // ── "Remember this" helpers ───────────────────────────────────────────────

    /**
     * Returns true if [input] is a "remember this" intent referring to the last
     * AI-fetched answer.
     */
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
     * Saves [lastAcquisitionAnswer] into permanent memory, keyed by [lastAcquisitionQuery].
     * The timestamp is stored inside the Memory metadata automatically.
     * Shows a confirmation message to the user.
     *
     * When the user later asks "when did I store this?" or "recall X", the RECALL
     * flow in [CoreStepExecutor] surfaces the saved date automatically.
     */
    private fun saveLastAnswerPermanently() {
        val answer = lastAcquisitionAnswer ?: return
        val query = lastAcquisitionQuery ?: "learned answer"

        viewModelScope.launch {
            val db = KhwabDatabase.getInstance(context)
            val permanentMemory = RoomPermanentMemory(db.permanentMemoryDao())

            val memory = Memory.createPermanent(
                subject = query.trim().lowercase(),
                value = answer,
                category = MemoryCategory.PREFERENCE,
                confidence = MemoryConfidence.EXPLICIT
            )
            permanentMemory.create(memory)

            lastAcquisitionAnswer = null
            lastAcquisitionQuery = null

            val confirmMsg = "🔒 Saved permanently! I'll always remember this answer about \"$query\".\n" +
                             "   You can ask me to recall it anytime, or say \"when did I save this\" to see the date."
            _uiState.update { state ->
                state.copy(
                    isTyping = false,
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
