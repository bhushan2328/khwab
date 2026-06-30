package com.toblad.khwab.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.toblad.khwab.R
import androidx.compose.foundation.layout.fillMaxSize



@Composable
fun MicButton(
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .size(140.dp)
            .clickable {
                onClick()
            },
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = KhwabBlue
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 12.dp
        )
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            KhwabBlueLight,
                            KhwabBlue,
                            KhwabBackground
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {

            Image(
                painter = painterResource(id = R.drawable.ic_mic),
                contentDescription = "Microphone",
                modifier = Modifier.size(60.dp)
            )

        }



    }

}