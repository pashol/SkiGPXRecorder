package com.skigpxrecorder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skigpxrecorder.R
import kotlinx.coroutines.launch

/**
 * Custom top bar for START screen with GPS indicator and menu.
 * Uses a gradient background that fades to transparent for map overlay.
 */
@Composable
fun StartScreenTopBar(
    gpsAccuracy: Float,
    drawerState: DrawerState? = null,
    elapsedTime: Long? = null,
    onSettingsClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onVersionClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.92f),
                        Color.White.copy(alpha = 0.75f),
                        Color.White.copy(alpha = 0f)
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Drawer Menu Button (left) or GPS Signal Indicator
        if (drawerState != null) {
            IconButton(onClick = {
                scope.launch {
                    drawerState.open()
                }
            }) {
                Icon(Icons.Default.Menu, contentDescription = "Open Menu")
            }
        } else {
            GpsSignalIndicator(accuracy = gpsAccuracy)
        }

        // Center text: timer when recording, app name otherwise
        if (elapsedTime != null) {
            Text(
                text = formatTopBarTime(elapsedTime),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // GPS Signal Indicator (right) when drawer is available, otherwise options menu
        if (drawerState != null) {
            GpsSignalIndicator(accuracy = gpsAccuracy)
        } else {
            IconButton(onClick = { showMenu = !showMenu }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu")
            }

            // Dropdown menu
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Settings") },
                    onClick = {
                        showMenu = false
                        onSettingsClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("About") },
                    onClick = {
                        showMenu = false
                        onAboutClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Version Info") },
                    onClick = {
                        showMenu = false
                        onVersionClick()
                    }
                )
            }
        }
    }
}

private fun formatTopBarTime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, secs)
}

/**
 * Displays GPS signal strength as colored bars (0-4)
 * - 4 bars (blue): <10m accuracy (excellent)
 * - 3 bars (light blue): 10-20m (good)
 * - 2 bars (yellow): 20-50m (fair)
 * - 1 bar (orange): 50-100m (weak)
 * - 0 bars (red): >100m (no signal)
 */
@Composable
private fun GpsSignalIndicator(accuracy: Float) {
    val (bars, color) = when {
        accuracy < 10f -> 4 to Color(0xFF1976D2)      // Excellent: Blue
        accuracy < 20f -> 3 to Color(0xFF42A5F5)      // Good: Light blue
        accuracy < 50f -> 2 to Color(0xFFFBC02D)      // Fair: Yellow
        accuracy < 100f -> 1 to Color(0xFFFF9800)     // Weak: Orange
        else -> 0 to Color(0xFFE53935)                 // No signal: Red
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(4) { index ->
            Icon(
                painter = painterResource(id = R.drawable.ic_signal_bar),
                contentDescription = null,
                modifier = Modifier.size(8.dp),
                tint = if (index < bars) color else Color.LightGray
            )
        }
    }
}
