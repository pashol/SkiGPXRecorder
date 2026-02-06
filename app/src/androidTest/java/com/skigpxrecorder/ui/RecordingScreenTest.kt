package com.skigpxrecorder.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.skigpxrecorder.ui.theme.SkiGPXRecorderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for RecordingScreen
 * These tests verify the UI components render correctly
 */
@RunWith(AndroidJUnit4::class)
class RecordingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun recordingScreen_initialState_showsStartButton() {
        // Given initial state
        val uiState = RecordingUiState(
            isRecording = false,
            hasLocationPermission = true,
            hasNotificationPermission = true
        )

        // When
        composeTestRule.setContent {
            SkiGPXRecorderTheme {
                RecordingScreen(
                    uiState = uiState,
                    onStartRecording = {},
                    onStopRecording = {},
                    onRequestLocationPermission = {},
                    onRequestNotificationPermission = {},
                    onDismissPermissionDenied = {},
                    onShareGpx = {},
                    onSaveToDownloads = {},
                    onNavigateToSession = {},
                    onResumeSession = {},
                    onDiscardSession = {},
                    onDismissBatteryWarning = {}
                )
            }
        }

        // Then - verify start button is visible
        composeTestRule.onNodeWithText("Start Recording").assertIsDisplayed()
    }

    @Test
    fun recordingScreen_recording_showsStopButton() {
        // Given recording state
        val uiState = RecordingUiState(
            isRecording = true,
            hasLocationPermission = true,
            hasNotificationPermission = true,
            startTime = System.currentTimeMillis(),
            elapsedTime = 120 // 2 minutes
        )

        // When
        composeTestRule.setContent {
            SkiGPXRecorderTheme {
                RecordingScreen(
                    uiState = uiState,
                    onStartRecording = {},
                    onStopRecording = {},
                    onRequestLocationPermission = {},
                    onRequestNotificationPermission = {},
                    onDismissPermissionDenied = {},
                    onShareGpx = {},
                    onSaveToDownloads = {},
                    onNavigateToSession = {},
                    onResumeSession = {},
                    onDiscardSession = {},
                    onDismissBatteryWarning = {}
                )
            }
        }

        // Then - verify stop button is visible
        composeTestRule.onNodeWithText("Stop Recording").assertIsDisplayed()
    }

    @Test
    fun recordingScreen_recording_displaysStats() {
        // Given recording state with stats
        val uiState = RecordingUiState(
            isRecording = true,
            hasLocationPermission = true,
            currentSpeed = 25.5f,
            distance = 5432.1f,
            elapsedTime = 1800, // 30 minutes
            maxSpeed = 45.2f,
            elevationGain = 234.5f
        )

        // When
        composeTestRule.setContent {
            SkiGPXRecorderTheme {
                RecordingScreen(
                    uiState = uiState,
                    onStartRecording = {},
                    onStopRecording = {},
                    onRequestLocationPermission = {},
                    onRequestNotificationPermission = {},
                    onDismissPermissionDenied = {},
                    onShareGpx = {},
                    onSaveToDownloads = {},
                    onNavigateToSession = {},
                    onResumeSession = {},
                    onDiscardSession = {},
                    onDismissBatteryWarning = {}
                )
            }
        }

        // Then - verify stats are displayed (exact text depends on formatting)
        // These assertions may need adjustment based on actual UI implementation
        composeTestRule.onNode(hasText("25.5", substring = true) or hasText("26", substring = true))
            .assertExists() // Current speed
    }

    @Test
    fun recordingScreen_permissionDenied_showsDialog() {
        // Given permission denied state
        val uiState = RecordingUiState(
            showPermissionDenied = true,
            hasLocationPermission = false
        )

        // When
        composeTestRule.setContent {
            SkiGPXRecorderTheme {
                RecordingScreen(
                    uiState = uiState,
                    onStartRecording = {},
                    onStopRecording = {},
                    onRequestLocationPermission = {},
                    onRequestNotificationPermission = {},
                    onDismissPermissionDenied = {},
                    onShareGpx = {},
                    onSaveToDownloads = {},
                    onNavigateToSession = {},
                    onResumeSession = {},
                    onDiscardSession = {},
                    onDismissBatteryWarning = {}
                )
            }
        }

        // Then - verify permission dialog is shown
        // The exact text depends on your dialog implementation
        composeTestRule.onNode(hasText("permission", ignoreCase = true, substring = true))
            .assertExists()
    }

    @Test
    fun recordingScreen_batteryWarning_showsAlert() {
        // Given battery warning state
        val uiState = RecordingUiState(
            isRecording = true,
            batteryWarning = true,
            hasLocationPermission = true
        )

        // When
        composeTestRule.setContent {
            SkiGPXRecorderTheme {
                RecordingScreen(
                    uiState = uiState,
                    onStartRecording = {},
                    onStopRecording = {},
                    onRequestLocationPermission = {},
                    onRequestNotificationPermission = {},
                    onDismissPermissionDenied = {},
                    onShareGpx = {},
                    onSaveToDownloads = {},
                    onNavigateToSession = {},
                    onResumeSession = {},
                    onDiscardSession = {},
                    onDismissBatteryWarning = {}
                )
            }
        }

        // Then - verify battery warning is shown
        composeTestRule.onNode(hasText("battery", ignoreCase = true, substring = true))
            .assertExists()
    }

    @Test
    fun recordingScreen_withMultipleRuns_showsRunCount() {
        // Given state with multiple runs
        val uiState = RecordingUiState(
            isRecording = true,
            hasLocationPermission = true,
            runCount = 5
        )

        // When
        composeTestRule.setContent {
            SkiGPXRecorderTheme {
                RecordingScreen(
                    uiState = uiState,
                    onStartRecording = {},
                    onStopRecording = {},
                    onRequestLocationPermission = {},
                    onRequestNotificationPermission = {},
                    onDismissPermissionDenied = {},
                    onShareGpx = {},
                    onSaveToDownloads = {},
                    onNavigateToSession = {},
                    onResumeSession = {},
                    onDiscardSession = {},
                    onDismissBatteryWarning = {}
                )
            }
        }

        // Then - verify run count is displayed
        composeTestRule.onNode(hasText("5", substring = true))
            .assertExists()
    }
}
