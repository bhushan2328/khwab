package com.toblad.khwab.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toblad.khwab.chat.model.ChatMessage
import com.toblad.khwab.chat.model.Sender
import com.toblad.khwab.ui.theme.KhwabBlue
import com.toblad.khwab.ui.theme.KhwabCard
import com.toblad.khwab.ui.theme.KhwabGray
import com.toblad.khwab.ui.theme.KhwabWhite

@Composable
fun ChatBubble(
    message: ChatMessage
) {

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
                containerColor = if (isUser) KhwabBlue else KhwabCard
            )
        ) {

            Box(
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                )
            ) {

                Text(
                    text = message.text,
                    color = KhwabWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

            }

        }

    }

}