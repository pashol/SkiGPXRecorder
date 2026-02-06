package com.skigpxrecorder.domain

import com.skigpxrecorder.data.model.SkiRun
import com.skigpxrecorder.data.model.TrackPoint

/**
 * Analyzes activity breakdown from track points
 * Classifies each point as: Skiing, Lift, Pause, or Walking
 */
object ActivityAnalyzer {

    enum class ActivityType {
        SKIING,  // Active skiing (in a detected run)
        LIFT,    // Ascending (lift or hiking up)
        PAUSE,   // Stationary (very low speed)
        WALKING  // Slow movement (not skiing or lift)
    }

    /**
     * Calculate activity breakdown from track points and detected runs
     * Returns time spent in each activity (milliseconds)
     */
    fun calculateActivityBreakdown(
        trackPoints: List<TrackPoint>,
        runs: List<SkiRun>
    ): Map<ActivityType, Long> {
        if (trackPoints.isEmpty()) {
            return mapOf(
                ActivityType.SKIING to 0L,
                ActivityType.LIFT to 0L,
                ActivityType.PAUSE to 0L,
                ActivityType.WALKING to 0L
            )
        }

        // Mark points that are part of ski runs
        val inRun = BooleanArray(trackPoints.size) { false }
        runs.forEach { run ->
            for (i in run.startIndex..run.endIndex) {
                if (i < inRun.size) {
                    inRun[i] = true
                }
            }
        }

        // Calculate time spent in each activity
        var skiingTime = 0L
        var liftTime = 0L
        var pauseTime = 0L
        var walkingTime = 0L

        for (i in 0 until trackPoints.size - 1) {
            val current = trackPoints[i]
            val next = trackPoints[i + 1]
            val duration = next.timestamp - current.timestamp

            // Classify based on speed and elevation change
            val speed = current.speed
            val elevationChange = next.elevation - current.elevation

            when {
                inRun[i] -> {
                    // Point is part of a ski run
                    skiingTime += duration
                }
                speed < 1.0f -> {
                    // Very slow or stationary
                    pauseTime += duration
                }
                elevationChange > 5.0 && speed < 10.0f -> {
                    // Ascending slowly (likely lift or hiking)
                    liftTime += duration
                }
                speed >= 1.0f && speed < 10.0f -> {
                    // Slow movement (walking)
                    walkingTime += duration
                }
                else -> {
                    // Default to walking for unclassified movement
                    walkingTime += duration
                }
            }
        }

        return mapOf(
            ActivityType.SKIING to skiingTime,
            ActivityType.LIFT to liftTime,
            ActivityType.PAUSE to pauseTime,
            ActivityType.WALKING to walkingTime
        )
    }

    /**
     * Convert activity breakdown to percentages
     */
    fun calculateActivityPercentages(
        activityBreakdown: Map<ActivityType, Long>
    ): Map<ActivityType, Float> {
        val totalTime = activityBreakdown.values.sum().toFloat()

        if (totalTime == 0f) {
            return mapOf(
                ActivityType.SKIING to 0f,
                ActivityType.LIFT to 0f,
                ActivityType.PAUSE to 0f,
                ActivityType.WALKING to 0f
            )
        }

        return mapOf(
            ActivityType.SKIING to (activityBreakdown[ActivityType.SKIING]!! / totalTime * 100),
            ActivityType.LIFT to (activityBreakdown[ActivityType.LIFT]!! / totalTime * 100),
            ActivityType.PAUSE to (activityBreakdown[ActivityType.PAUSE]!! / totalTime * 100),
            ActivityType.WALKING to (activityBreakdown[ActivityType.WALKING]!! / totalTime * 100)
        )
    }
}
