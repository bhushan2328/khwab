package com.toblad.khwab.speech

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.toblad.khwab.R

/**
 * Full-screen download screen shown on first launch when the Whisper
 * model files are not yet present on-device.
 *
 * [onReady] is called when the models are available (either already
 * downloaded or just completed) and the app can proceed normally.
 */
@Composable
fun ModelDownloadScreen(
    onReady: () -> Unit,
    viewModel: ModelDownloadViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // If models were already on disk, skip the screen immediately
    LaunchedEffect(state) {
        if (state is ModelDownloadState.Ready || state is ModelDownloadState.Completed) {
            onReady()
        }
    }

    // Auto-start download when screen first appears
    LaunchedEffect(Unit) {
        if (state is ModelDownloadState.Idle) {
            viewModel.startDownload()
        }
    }

    val colors = MaterialTheme.colorScheme

    // Logo pulse animation
    val logoTransition = rememberInfiniteTransition(label = "logo_pulse")
    val logoAlpha by logoTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_alpha"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Branded logo with pulse animation
                Image(
                    painter = painterResource(id = R.drawable.khwab_logo),
                    contentDescription = "Khwab",
                    modifier = Modifier
                        .size(96.dp)
                        .alpha(logoAlpha)
                )

                Text(
                    text = "Khwab",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.onBackground
                )

                Text(
                    text = "Your intelligent voice companion",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Setting up Khwab",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onBackground
                )

                when (val s = state) {

                    is ModelDownloadState.Idle,
                    is ModelDownloadState.Downloading -> {
                        val percent = (s as? ModelDownloadState.Downloading)?.percent ?: 0
                        val fileName = (s as? ModelDownloadState.Downloading)?.currentFile ?: ""

                        Text(
                            text = "Downloading speech recognition model…\nThis happens once and requires Wi-Fi.",
                            color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Rounded progress bar with inline percentage label
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { percent / 100f },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50)),
                                trackColor = colors.surfaceVariant,
                                color = colors.primary
                            )
                            Text(
                                text = "$percent%",
                                color = colors.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        if (fileName.isNotBlank()) {
                            Text(
                                text = fileName,
                                color = colors.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    is ModelDownloadState.Failed -> {
                        Text(
                            text = "Download failed",
                            color = colors.error,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = s.message,
                            color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = viewModel::retry,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Retry")
                        }
                    }

                    is ModelDownloadState.Ready,
                    is ModelDownloadState.Completed -> {
                        // LaunchedEffect above will call onReady() — nothing to show
                        Text(
                            text = "Ready!",
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}
