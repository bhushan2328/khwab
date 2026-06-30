package com.toblad.khwab.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.toblad.khwab.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.toblad.khwab.state.AssistantState
import com.toblad.khwab.state.AssistantStateManager










@Composable
fun HomeScreen(
    onStartClick: () -> Unit = {},
    onStopClick: () -> Unit = {}
) {

    val assistantState = AssistantStateManager.state
    val statusColor = when (assistantState) {
        AssistantState.STOPPED -> KhwabRed
        AssistantState.READY -> KhwabBlue
        AssistantState.RUNNING -> KhwabGreen
        AssistantState.LISTENING -> KhwabBlue
        AssistantState.THINKING -> KhwabYellow
        AssistantState.EXECUTING -> KhwabGreen
        AssistantState.SPEAKING -> KhwabBlue
        AssistantState.ERROR -> KhwabRed
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        KhwabBackground,
                        KhwabSurface
                    )
                )
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),

            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(30.dp))



            AnimatedKhwabLogo()

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Your Personal AI Assistant",
                color = KhwabGray,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            MicButton {

            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Tap to Activate",
                color = KhwabGray,
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            ActionButton(
                text = "START ASSISTANT",
                backgroundColor = KhwabGreen,
                onClick = onStartClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            ActionButton(
                text = "STOP ASSISTANT",
                backgroundColor = KhwabRed,
                onClick = onStopClick
            )

            Spacer(modifier = Modifier.height(30.dp))

            StatusCard(
                status = assistantState.name,
                statusColor = statusColor
            )

            Spacer(modifier = Modifier.height(20.dp))

            InfoCard()

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "Version 2.0",
                color = KhwabGray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun AnimatedKhwabLogo() {

    val transition = rememberInfiniteTransition(label = "logo")

    val scale by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Image(
        painter = painterResource(R.drawable.khwab_logo),
        contentDescription = "Khwab Logo",

        modifier = Modifier
            .size(130.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    )
}