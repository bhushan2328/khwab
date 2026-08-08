package com.toblad.khwab.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toblad.khwab.logging.LogReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    var errorLog by remember {
        mutableStateOf(LogReader.readError(context))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug Center", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface.copy(alpha = 0.95f), // fix #5: consistent opacity
                    titleContentColor = colors.onSurface
                )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {

            Text(
                text = "Latest Error Log",
                style = MaterialTheme.typography.labelMedium,
                color = colors.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Colorize log lines: lines containing "ERROR" shown in error color
                    val lines = errorLog.lines()
                    lines.forEach { line ->
                        val isError = line.contains("ERROR", ignoreCase = true)
                        // fix #6: background highlight on error lines for quick scanning
                        Text(
                            text = line,
                            modifier = if (isError) Modifier
                                .fillMaxWidth()
                                .background(
                                    colors.errorContainer.copy(alpha = 0.25f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp)
                            else Modifier,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = if (isError) colors.error else colors.onSurface,
                            fontWeight = if (isError) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { errorLog = LogReader.readError(context) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Refresh Log")
            }
        }
    }
}
