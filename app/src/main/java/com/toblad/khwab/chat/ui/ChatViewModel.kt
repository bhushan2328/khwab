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
import com.toblad.khwab.db.KhwabDatabase
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

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(input = text) }
    }

    fun sendMessage() {
        val input = uiState.value.input.trim()
        if (input.isBlank()) return

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
     * Suspends until the work reaches a terminal state, then:
     * - SUCCEEDED → reads the primary answer from output data and posts it
     *   as a new Khwab message; resets acquisition state to Completed.
     * - FAILED / CANCELLED → resets acquisition state to Failed so the
     *   "Learning…" strip disappears and the user is not left hanging.
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
}
