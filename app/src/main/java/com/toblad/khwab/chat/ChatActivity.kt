package com.toblad.khwab.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.toblad.khwab.chat.ui.ChatScreen
import com.toblad.khwab.chat.ui.ChatViewModel
import com.toblad.khwab.ui.theme.KhwabTheme

/**
 * Activity that hosts the Chat UI.
 */
class ChatActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            KhwabTheme {
                ChatScreen(onBackClick = { finish() })
            }
        }
    }

    /**
     * Cancel any in-flight execution loop when the user leaves ChatActivity
     * (e.g. an app was launched and they are now in that app). This prevents
     * the dynamic replan loop from continuing to run in the background and
     * re-issuing OPEN_APP (or other actions) when the user returns.
     */
    override fun onStop() {
        super.onStop()
        // Retrieve the existing ViewModel without creating a new one.
        val vm = ViewModelProvider(this)[ChatViewModel::class.java]
        vm.cancelExecution()
    }
}