package com.skigpxrecorder.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.skigpxrecorder.ui.RecordingScreen
import com.skigpxrecorder.ui.RecordingViewModel
import com.skigpxrecorder.ui.history.SessionHistoryScreen
import com.skigpxrecorder.ui.session.SessionScreen
import com.skigpxrecorder.ui.session.rundetail.RunDetailScreen

/**
 * Sealed class representing app navigation routes
 */
sealed class Screen(val route: String) {
    object Start : Screen("start")
    object SessionHistory : Screen("session_history")
    object Highscore : Screen("highscore")
    object Settings : Screen("settings")

    object Session : Screen("session/{sessionId}") {
        fun createRoute(sessionId: String) = "session/$sessionId"
    }

    object RunDetail : Screen("run_detail/{sessionId}/{runNumber}") {
        fun createRoute(sessionId: String, runNumber: Int) = "run_detail/$sessionId/$runNumber"
    }
}

/**
 * Main navigation host for the app
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    recordingViewModel: RecordingViewModel,
    onRequestPermission: () -> Unit,
    onOpenFile: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Start.route,
        modifier = modifier
    ) {
        composable(Screen.Start.route) {
            RecordingScreen(
                viewModel = recordingViewModel,
                navController = navController,
                onRequestPermission = onRequestPermission
            )
        }

        composable(
            route = Screen.Session.route,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            SessionScreen(
                sessionId = sessionId,
                navController = navController
            )
        }

        composable(
            route = Screen.RunDetail.route,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.StringType },
                navArgument("runNumber") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            val runNumber = backStackEntry.arguments?.getInt("runNumber") ?: return@composable

            RunDetailScreen(
                sessionId = sessionId,
                runNumber = runNumber,
                navController = navController
            )
        }

        composable(Screen.SessionHistory.route) {
            SessionHistoryScreen(
                navController = navController,
                onOpenFile = onOpenFile
            )
        }

        composable(Screen.Highscore.route) {
            // Placeholder for Highscore screen (Phase 4)
            HighscorePlaceholder(navController = navController)
        }

        composable(Screen.Settings.route) {
            com.skigpxrecorder.ui.settings.SettingsScreen(navController = navController)
        }
    }
}

/**
 * Placeholder screen for Highscore feature (to be implemented in Phase 4)
 */
@Composable
private fun HighscorePlaceholder(navController: NavController) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            androidx.compose.material3.Text(
                text = "HIGHSCORE",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            androidx.compose.material3.Text(
                text = "Coming in Phase 4",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
