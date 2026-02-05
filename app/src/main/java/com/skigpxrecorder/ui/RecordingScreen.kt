package com.skigpxrecorder.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.navigation.NavController
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skigpxrecorder.R
import com.skigpxrecorder.ui.theme.GreenSuccess
import com.skigpxrecorder.ui.theme.RedError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    viewModel: RecordingViewModel,
    navController: NavController? = null,
    onRequestPermission: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Main content without Scaffold - bottom nav is in MainActivity
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                // Status Card
                StatusCard(uiState)

                // Main Stats
                if (uiState.isRecording) {
                    RecordingStats(uiState)
                }

                Spacer(modifier = Modifier.weight(1f))

                // Control Buttons
                ControlButtons(
                    isRecording = uiState.isRecording,
                    onStartClick = { viewModel.startRecording() },
                    onStopClick = { viewModel.stopRecording() }
                )

                if (uiState.showShareOptions) {
                    ShareOptions(
                        onShareClick = { viewModel.shareGpxFile() },
                        onSaveClick = { viewModel.saveToDownloads() }
                    )
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
    }
}

@Composable
private fun StatusCard(uiState: RecordingUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (uiState.isRecording) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (uiState.isRecording) {
                    stringResource(R.string.recording_in_progress)
                } else {
                    stringResource(R.string.ready_to_record)
                },
                style = MaterialTheme.typography.headlineSmall,
                color = if (uiState.isRecording) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            if (uiState.isRecording) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatElapsedTime(uiState.elapsedTime),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(0.6f)
                )
            }
        }
    }
}

@Composable
private fun RecordingStats(uiState: RecordingUiState) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Primary stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = stringResource(R.string.current_speed),
                    value = "%.1f".format(uiState.currentSpeed),
                    unit = stringResource(R.string.km_h),
                    highlight = true
                )
                StatItem(
                    label = stringResource(R.string.distance),
                    value = "%.0f".format(uiState.distance),
                    unit = stringResource(R.string.meters)
                )
            }
            
            // Secondary stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = stringResource(R.string.elevation),
                    value = "%.0f".format(uiState.currentElevation),
                    unit = stringResource(R.string.meters)
                )
                StatItem(
                    label = stringResource(R.string.gps_accuracy),
                    value = "%.0f".format(uiState.gpsAccuracy),
                    unit = stringResource(R.string.meters)
                )
            }
            
            // More stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = stringResource(R.string.max_speed),
                    value = "%.1f".format(uiState.maxSpeed),
                    unit = stringResource(R.string.km_h)
                )
                StatItem(
                    label = stringResource(R.string.avg_speed),
                    value = "%.1f".format(uiState.avgSpeed),
                    unit = stringResource(R.string.km_h)
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
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    unit: String,
    highlight: Boolean = false
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
                style = if (highlight) {
                    MaterialTheme.typography.headlineMedium
                } else {
                    MaterialTheme.typography.headlineSmall
                },
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

@Composable
private fun ControlButtons(
    isRecording: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!isRecording) {
            Button(
                onClick = onStartClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenSuccess
                )
            ) {
                Text(
                    text = stringResource(R.string.start_recording),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        } else {
            Button(
                onClick = onStopClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RedError
                )
            ) {
                Text(
                    text = stringResource(R.string.stop_recording),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun ShareOptions(
    onShareClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
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
