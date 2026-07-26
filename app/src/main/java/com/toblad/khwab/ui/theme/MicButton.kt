package com.toblad.khwab.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.toblad.khwab.R

@Composable
fun MicButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    Card(
        modifier = modifier
            .size(140.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = KhwabBlue
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 14.dp
        )
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
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
                painter = painterResource(R.drawable.ic_mic),
                contentDescription = "Microphone",
                modifier = Modifier.size(72.dp)
            )

        }

    }

}