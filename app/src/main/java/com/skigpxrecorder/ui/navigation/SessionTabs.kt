package com.skigpxrecorder.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Session view tabs
 */
enum class SessionTab(
    val title: String,
    val icon: ImageVector
) {
    TRACK("Track", Icons.Default.List),
    MAP("Map", Icons.Default.LocationOn),
    ANALYSIS("Analysis", Icons.Default.Info),
    PROFILE("Profile", Icons.Default.Home)
}

/**
 * Bottom navigation bar for session tabs
 */
@Composable
fun SessionTabBar(
    selectedTab: SessionTab,
    onTabSelected: (SessionTab) -> Unit
) {
    NavigationBar {
        SessionTab.values().forEach { tab ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title
                    )
                },
                label = { Text(tab.title) }
            )
        }
    }
}
