package com.skigpxrecorder.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.skigpxrecorder.R
import com.skigpxrecorder.ui.components.GoogleMapsView
import com.skigpxrecorder.ui.components.HoldToStopButton
import com.skigpxrecorder.ui.components.StartScreenTopBar
import com.skigpxrecorder.ui.components.StatCircle
import com.skigpxrecorder.ui.theme.GreenSuccess
import com.skigpxrecorder.ui.theme.RedError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    viewModel: RecordingViewModel,
    navController: NavController? = null,
    onRequestPermission: () -> Unit,
    drawerState: DrawerState? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        // Layer 1: Full-screen Google Map (always visible)
        GoogleMapsView(
            trackPoints = uiState.trackPoints,
            hasLocationPermission = uiState.hasLocationPermission,
            modifier = Modifier.fillMaxSize()
        )

        // Layer 2: Semi-transparent top bar overlay
        StartScreenTopBar(
            gpsAccuracy = uiState.gpsAccuracy,
            drawerState = drawerState,
            elapsedTime = if (uiState.isRecording) uiState.elapsedTime else null,
            onSettingsClick = { /* TODO: Navigate to settings */ },
            onAboutClick = { /* TODO: Show about dialog */ },
            onVersionClick = { /* TODO: Show version dialog */ }
        )

        // Layer 3: Floating start button (idle, not sharing)
        if (!uiState.isRecording && !uiState.showShareOptions) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 32.dp)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Button(
                    onClick = { viewModel.startRecording() },
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenSuccess
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        text = stringResource(R.string.start_recording),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Layer 4: Bottom sheet with stats (recording)
        if (uiState.isRecording) {
            val sheetState = rememberStandardBottomSheetState(
                initialValue = SheetValue.PartiallyExpanded,
                skipHiddenState = true
            )
            val scaffoldState = rememberBottomSheetScaffoldState(
                bottomSheetState = sheetState
            )

            BottomSheetScaffold(
                scaffoldState = scaffoldState,
                sheetPeekHeight = 180.dp,
                sheetContainerColor = MaterialTheme.colorScheme.surface,
                sheetShadowElevation = 8.dp,
                sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                containerColor = Color.Transparent,
                sheetContent = {
                    RecordingSheetContent(
                        uiState = uiState,
                        onStopClick = { viewModel.stopRecording() }
                    )
                }
            ) {
                // Empty — map is behind in the Box
            }
        }

        // Layer 5: Post-recording share overlay
        AnimatedVisibility(
            visible = uiState.showShareOptions,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ShareOptionsOverlay(
                onShareClick = { viewModel.shareGpxFile() },
                onSaveClick = { viewModel.saveToDownloads() }
            )
        }

        // Save success toast
        if (uiState.showSaveSuccess) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = GreenSuccess
                    )
                ) {
                    Text(
                        text = stringResource(R.string.recording_saved),
                        modifier = Modifier.padding(16.dp),
                        color = Color.White
                    )
                }
            }
        }

        // Dialogs
        if (uiState.showPermissionDenied) {
            PermissionDeniedDialog(
                onDismiss = { viewModel.dismissPermissionDenied() },
                onOpenSettings = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            )
        }

        if (uiState.showResumeDialog) {
            ResumeRecordingDialog(
                onResume = { viewModel.onResumeSessionConfirmed() },
                onDiscard = { viewModel.onDiscardSession() }
            )
        }

        if (uiState.batteryWarning) {
            BatteryWarningDialog(
                onDismiss = { viewModel.dismissBatteryWarning() }
            )
        }
    }
}

/**
 * Bottom sheet content shown during recording.
 * Peek shows 3 key stats + hold-to-stop button.
 * Expanded shows all stats.
 */
@Composable
private fun RecordingSheetContent(
    uiState: RecordingUiState,
    onStopClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Drag handle indicator is built into BottomSheetScaffold

        // Peek content: 3 key stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CompactStatItem(
                value = "%.1f".format(uiState.currentSpeed),
                unit = stringResource(R.string.km_h),
                label = stringResource(R.string.current_speed)
            )
            CompactStatItem(
                value = "%.1f".format(uiState.distance / 1000),
                unit = "km",
                label = stringResource(R.string.distance)
            )
            CompactStatItem(
                value = "%.0f".format(uiState.currentElevation),
                unit = "m",
                label = stringResource(R.string.elevation)
            )
        }

        // Hold to stop button
        HoldToStopButton(
            onStopConfirmed = onStopClick,
            modifier = Modifier.fillMaxWidth()
        )

        // Expanded content: full stats
        Spacer(modifier = Modifier.height(4.dp))

        // Circular stat display (2x2 grid)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCircle(
                value = "%.1f".format(uiState.currentSpeed),
                unit = stringResource(R.string.km_h),
                label = stringResource(R.string.current_speed),
                progress = (uiState.currentSpeed / 80f).coerceIn(0f, 1f),
                size = 100.dp
            )
            StatCircle(
                value = "%.0f".format(uiState.distance / 1000),
                unit = "km",
                label = stringResource(R.string.distance),
                progress = (uiState.distance / 10000f).coerceIn(0f, 1f),
                size = 100.dp
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCircle(
                value = "%.0f".format(uiState.currentElevation),
                unit = "m",
                label = stringResource(R.string.elevation),
                progress = (uiState.currentElevation / 3000f).coerceIn(0f, 1f),
                size = 100.dp
            )
            StatCircle(
                value = "%.1f".format(uiState.maxSpeed),
                unit = stringResource(R.string.km_h),
                label = stringResource(R.string.max_speed),
                progress = (uiState.maxSpeed / 80f).coerceIn(0f, 1f),
                size = 100.dp
            )
        }

        // Secondary stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = stringResource(R.string.avg_speed),
                value = "%.1f".format(uiState.avgSpeed),
                unit = stringResource(R.string.km_h)
            )
            StatItem(
                label = stringResource(R.string.gps_accuracy),
                value = "%.0f".format(uiState.gpsAccuracy),
                unit = stringResource(R.string.meters)
            )
        }

        // Elevation gain/loss
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = stringResource(R.string.elevation_gain),
                value = "%.0f".format(uiState.elevationGain),
                unit = stringResource(R.string.meters)
            )
            StatItem(
                label = stringResource(R.string.elevation_loss),
                value = "%.0f".format(uiState.elevationLoss),
                unit = stringResource(R.string.meters)
            )
        }

        // Ski-specific stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = stringResource(R.string.ski_distance),
                value = "%.0f".format(uiState.skiDistance),
                unit = stringResource(R.string.meters)
            )
            StatItem(
                label = stringResource(R.string.ski_vertical),
                value = "%.0f".format(uiState.skiVertical),
                unit = stringResource(R.string.meters)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = stringResource(R.string.avg_ski_speed),
                value = "%.1f".format(uiState.avgSkiSpeed),
                unit = stringResource(R.string.km_h)
            )
            StatItem(
                label = stringResource(R.string.runs),
                value = uiState.runCount.toString(),
                unit = ""
            )
        }

        // Point count
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${uiState.pointCount} ${stringResource(R.string.points_recorded)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Compact stat item for the bottom sheet peek row.
 */
@Composable
private fun CompactStatItem(
    value: String,
    unit: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    unit: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Post-recording share/save overlay at bottom of screen.
 */
@Composable
private fun ShareOptionsOverlay(
    onShareClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .navigationBarsPadding(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.recording_complete),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onShareClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.share_gpx))
            }

            OutlinedButton(
                onClick = onSaveClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save_to_downloads))
            }
        }
    }
}

@Composable
private fun PermissionDeniedDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.location_permission_required)) },
        text = { Text(stringResource(R.string.location_permission_rationale)) },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.open_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.discard))
            }
        }
    )
}

@Composable
private fun ResumeRecordingDialog(
    onResume: () -> Unit,
    onDiscard: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text(stringResource(R.string.resume_recording)) },
        text = { Text(stringResource(R.string.resume_recording_message)) },
        confirmButton = {
            Button(onClick = onResume) {
                Text(stringResource(R.string.resume))
            }
        },
        dismissButton = {
            TextButton(onClick = onDiscard) {
                Text(stringResource(R.string.discard))
            }
        }
    )
}

@Composable
private fun BatteryWarningDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.low_battery_warning)) },
        text = { Text(stringResource(R.string.low_battery_message)) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

private fun formatElapsedTime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, secs)
}
