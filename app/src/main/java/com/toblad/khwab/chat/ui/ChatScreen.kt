package com.toblad.khwab.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.toblad.khwab.aura.ui.AuraScene
import com.toblad.khwab.ui.theme.AuraIconProvider
import com.toblad.khwab.ui.theme.ThemeController
import com.toblad.khwab.ui.theme.ThemeMode

@Composable
fun ChatScreen(
    onBackClick: () -> Unit = {},
    viewModel: ChatViewModel = viewModel()
) {
    val colors = MaterialTheme.colorScheme

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size, uiState.isTyping) {
        val extra = if (uiState.isTyping) 1 else 0
        val index = uiState.messages.size + extra - 1
        if (index >= 0) listState.animateScrollToItem(index)
    }

    val auraActive = ThemeController.currentTheme == ThemeMode.AURA
    val auraTheme = ThemeController.currentAuraTheme

    // When Aura is active the Scaffold itself is transparent — AuraScene
    // renders behind everything inside the content slot.
    val scaffoldBg = if (auraActive) Color.Transparent else colors.background

    Scaffold(
        containerColor = scaffoldBg,
        topBar = {
            Column {
                ChatTopBar(onBackClick = onBackClick)

                // Aura context strip — shows live weather + time phase when Aura is on
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
            // Aura visual scene — sky, sun/moon, weather, particles
            if (auraActive) {
                AuraScene(
                    theme = auraTheme,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Chat content on top of the scene
            Column(modifier = Modifier.fillMaxSize()) {

                // Centred date-separator chip
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surfaceVariant.copy(alpha = 0.70f)
                    ) {
                        Text(
                            text = "Today",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            color = colors.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(
                        horizontal = 12.dp,
                        vertical = 12.dp
                    )
                ) {
                    items(
                        items = uiState.messages,
                        key = { it.id }
                    ) { message ->
                        ChatBubble(message = message)
                    }

                    if (uiState.isTyping) {
                        item { TypingIndicator() }
                    }
                }
            }
        }
    }
}
