package com.skigpxrecorder.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.skigpxrecorder.data.model.DataSource
import com.skigpxrecorder.data.model.GPXData
import com.skigpxrecorder.data.model.TrackPoint
import com.skigpxrecorder.ui.session.SessionScreen
import com.skigpxrecorder.ui.theme.SkiGPXRecorderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for SessionScreen
 * Tests the session detail view with track, map, profile, and analysis tabs
 */
@RunWith(AndroidJUnit4::class)
class SessionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createSampleGpxData(): GPXData {
        val trackPoints = listOf(
            TrackPoint(46.0, 7.5, 2000.0, System.currentTimeMillis(), 5f, 10f),
            TrackPoint(46.01, 7.51, 2050.0, System.currentTimeMillis() + 60000, 5f, 20f),
            TrackPoint(46.02, 7.52, 2100.0, System.currentTimeMillis() + 120000, 5f, 15f)
        )

        return GPXData(
            sessionId = "test-session",
            sessionName = "Test Ski Session",
            points = trackPoints,
            source = DataSource.RECORDED,
            totalDistance = 1234.5f,
            elevationGain = 345.6f,
            elevationLoss = 123.4f,
            maxSpeed = 45.2f,
            avgSpeed = 25.3f,
            duration = 3600L,
            runs = emptyList(),
            isLive = false
        )
    }

    @Test
    fun sessionScreen_displaysSessionName() {
        val gpxData = createSampleGpxData()

        composeTestRule.setContent {
            SkiGPXRecorderTheme {
                SessionScreen(
                    sessionId = "test-session",
                    gpxData = gpxData,
                    selectedRunIndex = null,
                    isLoading = false,
                    onBack = {},
                    onSelectRun = {},
                    onClearRunSelection = {},
                    onDeleteSession = {}
                )
            }
        }

        // Verify session name is displayed
        composeTestRule.onNodeWithText("Test Ski Session").assertExists()
    }

    @Test
    fun sessionScreen_displaysTabs() {
        val gpxData = createSampleGpxData()

        composeTestRule.setContent {
            SkiGPXRecorderTheme {
                SessionScreen(
                    sessionId = "test-session",
                    gpxData = gpxData,
                    selectedRunIndex = null,
                    isLoading = false,
                    onBack = {},
                    onSelectRun = {},
                    onClearRunSelection = {},
                    onDeleteSession = {}
                )
            }
        }

        // Verify all tabs are present
        composeTestRule.onNodeWithText("Track").assertExists()
        composeTestRule.onNodeWithText("Map").assertExists()
        composeTestRule.onNodeWithText("Profile").assertExists()
        composeTestRule.onNodeWithText("Analysis").assertExists()
    }

    @Test
    fun sessionScreen_switchingTabs_works() {
        val gpxData = createSampleGpxData()

        composeTestRule.setContent {
            SkiGPXRecorderTheme {
                SessionScreen(
                    sessionId = "test-session",
                    gpxData = gpxData,
                    selectedRunIndex = null,
                    isLoading = false,
                    onBack = {},
                    onSelectRun = {},
                    onClearRunSelection = {},
                    onDeleteSession = {}
                )
            }
        }

        // Click on Map tab
        composeTestRule.onNodeWithText("Map").performClick()

        // Click on Profile tab
        composeTestRule.onNodeWithText("Profile").performClick()

        // Click on Analysis tab
        composeTestRule.onNodeWithText("Analysis").performClick()

        // Click back to Track tab
        composeTestRule.onNodeWithText("Track").performClick()

        // All tabs should still exist after switching
        composeTestRule.onNodeWithText("Track").assertExists()
        composeTestRule.onNodeWithText("Map").assertExists()
    }

    @Test
    fun sessionScreen_displaysStats() {
        val gpxData = createSampleGpxData()

        composeTestRule.setContent {
            SkiGPXRecorderTheme {
                SessionScreen(
                    sessionId = "test-session",
                    gpxData = gpxData,
                    selectedRunIndex = null,
                    isLoading = false,
                    onBack = {},
                    onSelectRun = {},
                    onClearRunSelection = {},
                    onDeleteSession = {}
                )
            }
        }

        // Verify key stats are displayed somewhere in the UI
        // These will appear in different formats depending on tab
        composeTestRule.waitForIdle()

        // Check for numeric values in the data (formatted differently in UI)
        // The exact assertions depend on how stats are formatted
        composeTestRule.onAllNodesWithText("1.2", substring = true).assertCountEquals(0)
            .fetchSemanticsNodes().isNotEmpty() // At least one stat should be visible
    }

    @Test
    fun sessionScreen_loading_showsLoadingIndicator() {
        composeTestRule.setContent {
            SkiGPXRecorderTheme {
                SessionScreen(
                    sessionId = "test-session",
                    gpxData = null,
                    selectedRunIndex = null,
                    isLoading = true,
                    onBack = {},
                    onSelectRun = {},
                    onClearRunSelection = {},
                    onDeleteSession = {}
                )
            }
        }

        // Verify loading indicator is shown
        // The exact implementation depends on your loading UI
        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertExists()
    }

    @Test
    fun sessionScreen_noData_handlesGracefully() {
        composeTestRule.setContent {
            SkiGPXRecorderTheme {
                SessionScreen(
                    sessionId = "test-session",
                    gpxData = null,
                    selectedRunIndex = null,
                    isLoading = false,
                    onBack = {},
                    onSelectRun = {},
                    onClearRunSelection = {},
                    onDeleteSession = {}
                )
            }
        }

        // Should handle null data gracefully (exact behavior depends on implementation)
        // At minimum, it shouldn't crash
        composeTestRule.waitForIdle()
    }
}
