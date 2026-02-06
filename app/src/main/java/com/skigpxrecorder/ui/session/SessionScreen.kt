package com.skigpxrecorder.ui.session

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.skigpxrecorder.util.Constants
import com.skigpxrecorder.data.local.UserPreferences
import com.skigpxrecorder.ui.navigation.SessionTab
import com.skigpxrecorder.ui.navigation.SessionTabBar
import com.skigpxrecorder.ui.session.trackview.TrackView

/**
 * Session screen with tab-based navigation
 * Container for TrackView, MapView, AnalysisView, ProfileView
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    sessionId: String,
    navController: NavController,
    viewModel: SessionViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(SessionTab.TRACK) }
    val gpxData by viewModel.gpxData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    val context = LocalContext.current

    // Load session data when screen is first composed
    LaunchedEffect(sessionId) {
        viewModel.loadSession(sessionId)
    }

    // Handle export state changes
    LaunchedEffect(exportState) {
        when (val state = exportState) {
            is SessionViewModel.ExportState.Success -> {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = Constants.GPX_MIME_TYPE
                    putExtra(Intent.EXTRA_STREAM, state.uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Ski Track GPX File")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(shareIntent, "Export GPX")
                )
                viewModel.resetExportState()
            }
            is SessionViewModel.ExportState.Error -> {
                android.util.Log.e("SessionScreen", "Export error: ${state.message}")
                // TODO: Show error snackbar
                viewModel.resetExportState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(gpxData?.metadata?.sessionName ?: "Session") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.exportSession(context) },
                        enabled = gpxData != null && exportState !is SessionViewModel.ExportState.Loading
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Export session")
                    }
                }
            )
        },
        bottomBar = {
            SessionTabBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                gpxData == null -> {
                    Text(
                        text = "No session data available",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    when (selectedTab) {
                        SessionTab.TRACK -> {
                            TrackView(
                                gpxData = gpxData!!,
                                unitSystem = UserPreferences.UnitSystem.METRIC, // Will be from preferences in Phase 10
                                onRunClick = { runIndex ->
                                    val run = gpxData!!.runs.getOrNull(runIndex)
                                    run?.let {
                                        navController.navigate(
                                            com.skigpxrecorder.ui.navigation.Screen.RunDetail.createRoute(
                                                sessionId,
                                                it.runNumber
                                            )
                                        )
                                    }
                                }
                            )
                        }
                        SessionTab.MAP -> {
                            com.skigpxrecorder.ui.session.mapview.MapView(
                                gpxData = gpxData!!,
                                unitSystem = UserPreferences.UnitSystem.METRIC
                            )
                        }
                        SessionTab.ANALYSIS -> {
                            com.skigpxrecorder.ui.session.analysis.AnalysisView(
                                gpxData = gpxData!!,
                                unitSystem = UserPreferences.UnitSystem.METRIC
                            )
                        }
                        SessionTab.PROFILE -> {
                            com.skigpxrecorder.ui.session.profile.ProfileView(
                                gpxData = gpxData!!,
                                unitSystem = UserPreferences.UnitSystem.METRIC
                            )
                        }
                    }
                }
            }
        }
    }
}
