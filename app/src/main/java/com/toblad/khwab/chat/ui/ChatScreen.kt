package com.toblad.khwab.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toblad.khwab.chat.model.ChatMessage
import com.toblad.khwab.chat.model.Sender
import com.toblad.khwab.ui.theme.KhwabBackground
import com.toblad.khwab.ui.theme.KhwabWhite

@Composable
fun ChatScreen(
    onBackClick: () -> Unit = {}
) {

    val messages = remember {
        listOf(
            ChatMessage(
                id = 1,
                text = "Hello Mr. Bhushan! I'm Khwab. How can I help you today?",
                sender = Sender.KHWAB
            ),
            ChatMessage(
                id = 2,
                text = "Open Chrome",
                sender = Sender.USER
            ),
            ChatMessage(
                id = 3,
                text = "Sure! Opening Chrome...",
                sender = Sender.KHWAB
            )
        )
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
                text = "",
                onTextChange = {},
                onSendClick = {},
                onMicClick = {}
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(
                    horizontal = 12.dp,
                    vertical = 12.dp
                )
            ) {

                items(messages) { message ->
                    ChatBubble(message = message)
                }

                item {
                    TypingIndicator()
                }
            }
        }
    }
}