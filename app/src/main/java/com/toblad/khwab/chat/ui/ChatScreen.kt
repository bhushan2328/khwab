package com.toblad.khwab.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.toblad.khwab.ui.theme.KhwabBackground
import com.toblad.khwab.ui.theme.KhwabWhite

@Composable
fun ChatScreen(
    onBackClick: () -> Unit = {},
    viewModel: ChatViewModel = viewModel()
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size, uiState.isTyping) {
        val extra = if (uiState.isTyping) 1 else 0
        val index = uiState.messages.size + extra - 1

        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }

    Scaffold(
        containerColor = KhwabBackground,
        topBar = {
            ChatTopBar(
                onBackClick = onBackClick
            )
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            Text(
                text = "Today",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                color = KhwabWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )

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
                    item {
                        TypingIndicator()
                    }
                }
            }
        }
    }
}