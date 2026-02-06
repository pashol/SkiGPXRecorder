package com.skigpxrecorder.ui.highscore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skigpxrecorder.data.repository.GpxRepository
import com.skigpxrecorder.domain.ActivityAnalyzer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Data class for activity breakdown statistics
 */
data class ActivityStat(
    val label: String,
    val percentage: Float,
    val duration: Long // milliseconds
)

/**
 * Data class for speed distribution buckets
 */
data class SpeedBucket(
    val range: String,
    val count: Int
)

/**
 * UI state for Highscore screen
 */
data class HighscoreUiState(
    val maxSpeed: Float = 0f,
    val totalRideTime: Long = 0L, // milliseconds
    val totalDistanceDownhill: Float = 0f, // meters
    val totalSessions: Int = 0,
    val maxElevation: Double = 0.0,
    val totalVerticalDescent: Float = 0f,
    val activityBreakdown: List<ActivityStat> = emptyList(),
    val speedDistribution: List<SpeedBucket> = emptyList()
)

/**
 * ViewModel for Highscore/Statistics screen
 */
@HiltViewModel
class HighscoreViewModel @Inject constructor(
    private val gpxRepository: GpxRepository
) : ViewModel() {

    /**
     * UI state with aggregated statistics
     */
    val uiState: StateFlow<HighscoreUiState> = gpxRepository
        .getCompletedSessions()
        .map { sessions ->
            if (sessions.isEmpty()) {
                HighscoreUiState()
            } else {
                // Calculate aggregated statistics
                val maxSpeed = sessions.maxOfOrNull { it.maxSpeed } ?: 0f
                val totalRideTime = sessions.sumOf { session ->
                    session.endTime?.let { (it - session.startTime) } ?: 0L
                }
                val totalDistanceDownhill = sessions.sumOf { it.distance.toDouble() }.toFloat()
                val maxElevation = sessions.maxOfOrNull { it.elevationGain.toDouble() } ?: 0.0
                val totalVerticalDescent = sessions.sumOf { it.elevationGain.toDouble() }.toFloat()

                // Calculate actual activity breakdown from all sessions
                val activityBreakdown = calculateActivityBreakdown(sessions)

                // Calculate actual speed distribution from all sessions
                val speedDistribution = calculateSpeedDistribution(sessions)

                HighscoreUiState(
                    maxSpeed = maxSpeed,
                    totalRideTime = totalRideTime,
                    totalDistanceDownhill = totalDistanceDownhill,
                    totalSessions = sessions.size,
                    maxElevation = maxElevation,
                    totalVerticalDescent = totalVerticalDescent,
                    activityBreakdown = activityBreakdown,
                    speedDistribution = speedDistribution
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HighscoreUiState()
        )

    /**
     * Calculate activity breakdown from all sessions
     */
    private suspend fun calculateActivityBreakdown(sessions: List<com.skigpxrecorder.data.model.RecordingSession>): List<ActivityStat> {
        // Aggregate activity time across all sessions
        var totalSkiingTime = 0L
        var totalLiftTime = 0L
        var totalPauseTime = 0L
        var totalWalkingTime = 0L

        sessions.forEach { session ->
            // Get track points and runs for this session
            val trackPoints = gpxRepository.getTrackPoints(session.id)
            val runs = gpxRepository.getRunsForSession(session.id)

            // Calculate activity breakdown for this session
            val breakdown = ActivityAnalyzer.calculateActivityBreakdown(trackPoints, runs)

            totalSkiingTime += breakdown[ActivityAnalyzer.ActivityType.SKIING] ?: 0L
            totalLiftTime += breakdown[ActivityAnalyzer.ActivityType.LIFT] ?: 0L
            totalPauseTime += breakdown[ActivityAnalyzer.ActivityType.PAUSE] ?: 0L
            totalWalkingTime += breakdown[ActivityAnalyzer.ActivityType.WALKING] ?: 0L
        }

        val totalTime = totalSkiingTime + totalLiftTime + totalPauseTime + totalWalkingTime

        if (totalTime == 0L) {
            return emptyList()
        }

        // Calculate percentages
        return listOf(
            ActivityStat(
                "Skiing",
                (totalSkiingTime.toFloat() / totalTime * 100),
                totalSkiingTime
            ),
            ActivityStat(
                "Lift",
                (totalLiftTime.toFloat() / totalTime * 100),
                totalLiftTime
            ),
            ActivityStat(
                "Pause",
                (totalPauseTime.toFloat() / totalTime * 100),
                totalPauseTime
            ),
            ActivityStat(
                "Walking",
                (totalWalkingTime.toFloat() / totalTime * 100),
                totalWalkingTime
            )
        )
    }

    /**
     * Calculate speed distribution from all sessions
     */
    private suspend fun calculateSpeedDistribution(sessions: List<com.skigpxrecorder.data.model.RecordingSession>): List<SpeedBucket> {
        // Define speed buckets (km/h)
        val buckets = mutableListOf(
            0 to 0,  // 0-10
            0 to 0,  // 10-20
            0 to 0,  // 20-30
            0 to 0,  // 30-40
            0 to 0,  // 40-50
            0 to 0   // 50+
        )

        // Count track points in each bucket across all sessions
        sessions.forEach { session ->
            val trackPoints = gpxRepository.getTrackPoints(session.id)

            trackPoints.forEach { point ->
                val speed = point.speed

                when {
                    speed < 10f -> buckets[0] = buckets[0].first to buckets[0].second + 1
                    speed < 20f -> buckets[1] = buckets[1].first to buckets[1].second + 1
                    speed < 30f -> buckets[2] = buckets[2].first to buckets[2].second + 1
                    speed < 40f -> buckets[3] = buckets[3].first to buckets[3].second + 1
                    speed < 50f -> buckets[4] = buckets[4].first to buckets[4].second + 1
                    else -> buckets[5] = buckets[5].first to buckets[5].second + 1
                }
            }
        }

        return listOf(
            SpeedBucket("0-10 km/h", buckets[0].second),
            SpeedBucket("10-20 km/h", buckets[1].second),
            SpeedBucket("20-30 km/h", buckets[2].second),
            SpeedBucket("30-40 km/h", buckets[3].second),
            SpeedBucket("40-50 km/h", buckets[4].second),
            SpeedBucket("50+ km/h", buckets[5].second)
        )
    }
}
