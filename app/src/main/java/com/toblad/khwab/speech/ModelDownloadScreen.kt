package com.toblad.khwab.speech

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

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

                Text(
                    text = "Setting up Khwab",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground
                )

                Spacer(modifier = Modifier.height(4.dp))

                when (val s = state) {

                    is ModelDownloadState.Idle,
                    is ModelDownloadState.Downloading -> {
                        val percent = (s as? ModelDownloadState.Downloading)?.percent ?: 0
                        val fileName = (s as? ModelDownloadState.Downloading)?.currentFile ?: ""

                        Text(
                            text = "Downloading speech recognition model…\nThis happens once and requires Wi-Fi.",
                            color = colors.onSurfaceVariant,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { percent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            trackColor = colors.surfaceVariant,
                            color = colors.primary
                        )

                        Text(
                            text = if (fileName.isNotBlank()) "$percent% — $fileName"
                                   else "$percent%",
                            color = colors.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }

                    is ModelDownloadState.Failed -> {
                        Text(
                            text = "Download failed",
                            color = colors.error,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = s.message,
                            color = colors.onSurfaceVariant,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = viewModel::retry,
                            shape = RoundedCornerShape(12.dp)
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
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }
}
