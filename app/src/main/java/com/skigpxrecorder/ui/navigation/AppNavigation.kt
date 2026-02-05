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
import com.skigpxrecorder.ui.highscore.HighscoreScreen
import com.skigpxrecorder.ui.history.MapAllSessionsScreen
import com.skigpxrecorder.ui.history.SessionHistoryScreen
import com.skigpxrecorder.ui.session.SessionScreen
import com.skigpxrecorder.ui.session.rundetail.RunDetailScreen

/**
 * Sealed class representing app navigation routes
 */
sealed class Screen(val route: String) {
    object Start : Screen("start")
    object SessionHistory : Screen("session_history")
    object MapAllSessions : Screen("map_all_sessions")
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

        composable(Screen.MapAllSessions.route) {
            MapAllSessionsScreen(navController = navController)
        }

        composable(Screen.Highscore.route) {
            HighscoreScreen(navController = navController)
        }

        composable(Screen.Settings.route) {
            com.skigpxrecorder.ui.settings.SettingsScreen(navController = navController)
        }
    }
}
