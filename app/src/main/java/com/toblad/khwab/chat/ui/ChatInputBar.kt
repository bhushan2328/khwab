package com.toblad.khwab.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toblad.khwab.ui.theme.KhwabBlue
import com.toblad.khwab.ui.theme.KhwabGray
import com.toblad.khwab.ui.theme.KhwabWhite

@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onMicClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text("Type a message...")
            },
            maxLines = 5,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = KhwabBlue,
                unfocusedBorderColor = KhwabGray,
                focusedTextColor = KhwabWhite,
                unfocusedTextColor = KhwabWhite,
                cursorColor = KhwabBlue
            ),
            textStyle = MaterialTheme.typography.bodyLarge
        )

        IconButton(
            onClick = onMicClick
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Voice",
                tint = KhwabBlue
            )
        }

        IconButton(
            onClick = onSendClick
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send",
                tint = KhwabBlue
            )
        }
    }
}