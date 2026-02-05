package com.skigpxrecorder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skigpxrecorder.R

/**
 * Custom top bar for START screen with GPS indicator and menu
 */
@Composable
fun StartScreenTopBar(
    gpsAccuracy: Float,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onVersionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.85f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // GPS Signal Indicator (left)
        GpsSignalIndicator(accuracy = gpsAccuracy)

        // App Logo/Title (center)
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // Menu Button (right)
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
