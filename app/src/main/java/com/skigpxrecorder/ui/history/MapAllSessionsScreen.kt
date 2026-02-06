package com.skigpxrecorder.ui.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.skigpxrecorder.R
import com.skigpxrecorder.data.model.RecordingSession
import com.skigpxrecorder.data.repository.GpxRepository
import com.skigpxrecorder.ui.navigation.Screen
import com.skigpxrecorder.ui.session.mapview.OsmMapWrapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import javax.inject.Inject

/**
 * Screen showing all recorded sessions on a map
 * Each session is represented by a marker at its starting location
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapAllSessionsScreen(
    navController: NavController,
    viewModel: SessionHistoryViewModel = hiltViewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Sessions Map") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Just show the map - marker setup happens in onMapReady
            OsmMapWrapper { mapView ->
                // Note: Actual marker setup would happen asynchronously
                // For now, the map shows without session markers as a placeholder
            }
        }
    }
}

/**
 * Format session date for marker title
 */
private fun formatSessionDate(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

/**
 * Format session stats for marker snippet
 */
private fun formatSessionStats(session: RecordingSession): String {
    val distance = String.format("%.2f km", session.distance / 1000)
    val vertical = String.format("%.0f m", session.elevationGain)
    val runs = "${session.runsCount} runs"
    return "$distance • $vertical • $runs"
}
