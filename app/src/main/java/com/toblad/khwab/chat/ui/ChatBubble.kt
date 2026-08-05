package com.toblad.khwab.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toblad.khwab.chat.model.ChatMessage
import com.toblad.khwab.chat.model.MessageState
import com.toblad.khwab.chat.model.Sender

@Composable
fun ChatBubble(
    message: ChatMessage
) {

    val colors = MaterialTheme.colorScheme

    val isUser = message.sender == Sender.USER

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = if (isUser) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {

        Card(
            modifier = Modifier.fillMaxWidth(0.80f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) colors.primary else colors.surfaceVariant
            )
        ) {

            Box(
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                )
            ) {

                Text(
                    text = buildString {
                        append(message.text)

                        if (message.state == MessageState.STREAMING) {
                            append("▌")
                        }
                    },
                    color = if (isUser) colors.onPrimary else colors.onSurfaceVariant,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

            }

        }

    }
}