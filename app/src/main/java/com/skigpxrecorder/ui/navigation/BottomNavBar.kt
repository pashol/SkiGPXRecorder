package com.skigpxrecorder.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController

/**
 * Main navigation tabs
 */
enum class MainTab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HISTORY("session_history", "HISTORIE", Icons.Default.List),
    START("start", "START", Icons.Default.Home),
    HIGHSCORE("highscore", "HIGHSCORE", Icons.Default.Star)
}

/**
 * Bottom navigation bar component for main app navigation
 */
@Composable
fun BottomNavBar(
    currentRoute: String?,
    navController: NavController,
    onTabSelected: (MainTab) -> Unit = {}
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        MainTab.entries.forEach { tab ->
            val selected = currentRoute == tab.route

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                selected = selected,
                onClick = {
                    if (currentRoute != tab.route) {
                        onTabSelected(tab)
                        navController.navigate(tab.route) {
                            // Pop up to start destination to avoid building up back stack
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}
