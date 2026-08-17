package com.toblad.khwab.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toblad.khwab.aura.model.TimePhase
import com.toblad.khwab.ui.theme.ThemeController
import com.toblad.khwab.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    onBackClick: () -> Unit = {},
    onClearChat: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    var menuExpanded by remember { mutableStateOf(false) }

    val auraActive = ThemeController.currentTheme == ThemeMode.AURA
    val isDaytimeAura = auraActive && ThemeController.currentAuraTheme.timePhase in listOf(
        TimePhase.SUNRISE, TimePhase.MORNING, TimePhase.NOON, TimePhase.AFTERNOON
    )

    val containerColor = when {
        isDaytimeAura -> Color.White.copy(alpha = 0.72f)   // frosted white over bright sky
        auraActive    -> colors.surface.copy(alpha = 0.78f) // semi-transparent for night/dusk/dawn
        else          -> colors.surface
    }
    // Ensure readable contrast for both daytime frosted and default dark
    val contentColor = if (isDaytimeAura) Color(0xFF1A1A2E) else colors.onSurface
    val subtitleColor = if (isDaytimeAura) Color(0xFF3D3D5C).copy(alpha = 0.75f)
                        else colors.onSurfaceVariant

    TopAppBar(
        windowInsets = WindowInsets.statusBars,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            titleContentColor = contentColor,
            navigationIconContentColor = contentColor,
            actionIconContentColor = contentColor
        ),
        navigationIcon = {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Assistant avatar badge
                Surface(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(9.dp)),
                    color = if (isDaytimeAura) colors.primary.copy(alpha = 0.18f)
                            else colors.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Text(
                        text = "Khwab",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        lineHeight = 18.sp
                    )
                    Text(
                        text = "AI assistant",
                        style = MaterialTheme.typography.labelSmall,
                        color = subtitleColor,
                        lineHeight = 14.sp,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        },
        actions = {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options"
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Clear chat") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onClearChat()
                    }
                )
            }
        }
    )
}
