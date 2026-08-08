package com.toblad.khwab.chat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.toblad.khwab.aura.ui.AuraScene
import com.toblad.khwab.background.KnowledgeAcquisitionState
import com.toblad.khwab.chat.model.ChatMessage
import com.toblad.khwab.ui.theme.AuraIconProvider
import com.toblad.khwab.ui.theme.ThemeController
import com.toblad.khwab.ui.theme.ThemeMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    onBackClick: () -> Unit = {},
    viewModel: ChatViewModel = viewModel()
) {
    val colors = MaterialTheme.colorScheme

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val acquisitionState by viewModel.acquisitionState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Show the FAB only when the user has scrolled away from the bottom
    val showScrollFab by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val totalItems = listState.layoutInfo.totalItemsCount
            totalItems > 0 && lastVisible < totalItems - 1
        }
    }

    LaunchedEffect(uiState.messages.size, uiState.isTyping) {
        val extra = if (uiState.isTyping) 1 else 0
        val index = uiState.messages.size + extra - 1
        if (index >= 0) listState.animateScrollToItem(index)
    }

    val auraActive = ThemeController.currentTheme == ThemeMode.AURA
    val auraTheme = ThemeController.currentAuraTheme

    val scaffoldBg = if (auraActive) Color.Transparent else colors.background

    // Group messages by calendar day for date-separator chips
    val groupedItems = remember(uiState.messages) {
        buildGroupedItems(uiState.messages)
    }

    Scaffold(
        containerColor = scaffoldBg,
        topBar = {
            Column {
                ChatTopBar(
                    onBackClick = onBackClick,
                    onClearChat = viewModel::clearChat
                )

                // ── Knowledge acquisition progress bar ─────────────────────────
                // Replaces the old plain-text "Learning: …" strip with a visible
                // indeterminate LinearProgressIndicator + label row.
                AnimatedVisibility(
                    visible = acquisitionState is KnowledgeAcquisitionState.Acquiring,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    val query = (acquisitionState as? KnowledgeAcquisitionState.Acquiring)?.query
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.primaryContainer.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (query != null) "Learning: $query…" else "Learning…",
                                color = colors.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.primary,
                            trackColor = colors.primaryContainer.copy(alpha = 0.30f)
                        )
                    }
                }

                // ── Aura context strip ─────────────────────────────────────────
                if (auraActive) {
                    val contextIcon = AuraIconProvider.micIconFor(
                        weather = auraTheme.weatherState,
                        timePhase = auraTheme.timePhase
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = contextIcon,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.padding(end = 2.dp)
                        )
                        Text(
                            text = buildString {
                                append(auraTheme.weatherState.name
                                    .lowercase().replaceFirstChar { it.uppercase() })
                                append("  •  ")
                                append(auraTheme.timePhase.name
                                    .replace("_", " ")
                                    .lowercase().replaceFirstChar { it.uppercase() })
                            },
                            color = colors.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        bottomBar = {
            ChatInputBar(
                text = uiState.input,
                onTextChange = viewModel::onInputChanged,
                onSendClick = viewModel::sendMessage,
                onMicClick = {
                    // Sherpa integration comes here
                }
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (auraActive) {
                AuraScene(
                    theme = auraTheme,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (groupedItems.isEmpty() && !uiState.isTyping) {
                // ── Empty state ─────────────────────────────────────────────
                ChatEmptyState(onSuggestionClick = viewModel::onInputChanged)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(
                        horizontal = 12.dp,
                        vertical = 12.dp
                    )
                ) {
                    groupedItems.forEach { item ->
                        when (item) {
                            is ChatListItem.DateHeader -> {
                                item(key = "date_${item.label}") {
                                    DateSeparatorChip(label = item.label)
                                }
                            }
                            is ChatListItem.MessageItem -> {
                                item(key = item.message.id) {
                                    ChatBubble(message = item.message)
                                }
                            }
                        }
                    }

                    if (uiState.isTyping) {
                        item(key = "typing") { TypingIndicator() }
                    }
                }
            }

            // ── Scroll-to-bottom FAB — always a direct child of the outer Box ─
            AnimatedVisibility(
                visible = showScrollFab,
                enter = scaleIn(tween(200)) + fadeIn(tween(200)),
                exit  = scaleOut(tween(160)) + fadeOut(tween(160)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 12.dp, end = 16.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            val target = listState.layoutInfo.totalItemsCount - 1
                            if (target >= 0) listState.animateScrollToItem(target)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor   = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Scroll to bottom",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// ── Date separator chip ───────────────────────────────────────────────────────

@Composable
private fun DateSeparatorChip(label: String) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colors.surfaceVariant.copy(alpha = 0.70f)
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = colors.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

// ── Grouped list model ────────────────────────────────────────────────────────

private sealed class ChatListItem {
    data class DateHeader(val label: String) : ChatListItem()
    data class MessageItem(val message: ChatMessage) : ChatListItem()
}

/**
 * Groups [messages] by calendar day and inserts a [ChatListItem.DateHeader]
 * before each new day, producing labels like "Today", "Yesterday", or "Mon, 12 Jan".
 */
private fun buildGroupedItems(messages: List<ChatMessage>): List<ChatListItem> {
    if (messages.isEmpty()) return emptyList()

    val result = mutableListOf<ChatListItem>()
    val todayCal = Calendar.getInstance()
    val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val labelFmt = SimpleDateFormat("EEE, d MMM", Locale.getDefault())

    fun Calendar.dateKey(): String =
        "${get(Calendar.YEAR)}-${get(Calendar.DAY_OF_YEAR)}"

    val todayKey = todayCal.dateKey()
    val yesterdayKey = yesterdayCal.dateKey()

    var lastDayKey: String? = null
    val msgCal = Calendar.getInstance()

    messages.forEach { msg ->
        msgCal.timeInMillis = msg.timestamp
        val dayKey = msgCal.dateKey()
        if (dayKey != lastDayKey) {
            val label = when (dayKey) {
                todayKey -> "Today"
                yesterdayKey -> "Yesterday"
                else -> labelFmt.format(Date(msg.timestamp))
            }
            result += ChatListItem.DateHeader(label)
            lastDayKey = dayKey
        }
        result += ChatListItem.MessageItem(msg)
    }

    return result
}

// ── Empty state ───────────────────────────────────────────────────────────────

private val suggestionChips = listOf(
    "What's the weather today?",
    "Set a reminder",
    "Tell me a joke"
)

@Composable
private fun ChatEmptyState(onSuggestionClick: (String) -> Unit = {}) {
    val colors = MaterialTheme.colorScheme

    // Breathing animation for the icon badge
    val breathTransition = rememberInfiniteTransition(label = "empty_breath")
    val breathScale by breathTransition.animateFloat(
        initialValue = 1.0f,
        targetValue  = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badge_scale"
    )

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(400)) + slideInVertically(
            animationSpec = tween(400),
            initialOffsetY = { it / 5 }
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 40.dp)
            ) {
                // Breathing icon badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.primaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.scale(breathScale)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier
                            .padding(20.dp)
                            .size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Start a conversation",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Ask me anything — I'm your intelligent voice companion.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Suggestion chips — tap to pre-fill the input
                suggestionChips.forEach { suggestion ->
                    SuggestionChip(
                        onClick = { onSuggestionClick(suggestion) },
                        label = {
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )
                }
            }
        }
    }
}
