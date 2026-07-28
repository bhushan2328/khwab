package com.toblad.khwab.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.toblad.khwab.chat.ui.ChatScreen
import com.toblad.khwab.ui.theme.KhwabTheme

/**
 * Activity that hosts the Chat UI.
 */
class ChatActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            KhwabTheme {

                ChatScreen(

                    onBackClick = {
                        finish()
                    }

                )

            }

        }
    }
}