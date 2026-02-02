package com.skigpxrecorder.domain

import com.skigpxrecorder.data.model.DataSource
import com.skigpxrecorder.data.model.GPXData
import com.skigpxrecorder.data.model.SessionMetadata
import com.skigpxrecorder.data.model.SessionStats
import com.skigpxrecorder.data.model.TrackPoint
import kotlin.math.max
import kotlin.math.min

/**
 * Analyzes GPS track points to compute comprehensive session statistics
 */
object SessionAnalyzer {

    /**
     * Analyze a complete session and return GPXData with runs, stats, and metadata
     */
    fun analyzeSession(
        sessionId: String,
        sessionName: String,
        points: List<TrackPoint>,
        source: DataSource = DataSource.RECORDED,
        isLive: Boolean = false,
        existingRuns: List<com.skigpxrecorder.data.model.SkiRun>? = null
    ): GPXData {
        if (points.isEmpty()) {
            return GPXData(
                trackPoints = emptyList(),
                runs = emptyList(),
                stats = SessionStats(),
                metadata = SessionMetadata(
                    sessionId = sessionId,
                    sessionName = sessionName,
                    startTime = System.currentTimeMillis(),
                    endTime = null,
                    isLive = isLive
                ),
                source = source
            )
        }

        // Use provided runs or detect from points
        val runs = existingRuns ?: RunDetector.detectRuns(points)

        // Calculate statistics
        val stats = calculateSessionStats(points, runs)

        // Build metadata
        val metadata = SessionMetadata(
            sessionId = sessionId,
            sessionName = sessionName,
            startTime = points.first().timestamp,
            endTime = if (!isLive) points.last().timestamp else null,
            isLive = isLive
        )

        return GPXData(
            trackPoints = points,
            runs = runs,
            stats = stats,
            metadata = metadata,
            source = source
        )
    }

    /**
     * Calculate comprehensive session statistics
     */
    private fun calculateSessionStats(
        points: List<TrackPoint>,
        runs: List<com.skigpxrecorder.data.model.SkiRun>
    ): SessionStats {
        if (points.isEmpty()) return SessionStats()

        // Calculate total distance and elevation metrics
        var totalDistance = 0f
        var totalAscent = 0f
        var totalDescent = 0f
        var maxSpeed = 0f
        var maxAltitude = points.first().elevation.toFloat()
        var minAltitude = points.first().elevation.toFloat()
        var totalSpeed = 0f
        var speedCount = 0

        // Heart rate metrics
        var totalHeartRate = 0f
        var maxHeartRate = 0f
        var hrCount = 0

        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]

            // Distance
            totalDistance += StatsCalculator.calculateDistance(prev, curr).toFloat()

            // Elevation
            val elevDiff = (curr.elevation - prev.elevation).toFloat()
            if (elevDiff > 0) totalAscent += elevDiff
            else totalDescent += kotlin.math.abs(elevDiff)

            maxAltitude = max(maxAltitude, curr.elevation.toFloat())
            minAltitude = min(minAltitude, curr.elevation.toFloat())

            // Speed
            if (curr.speed > maxSpeed) maxSpeed = curr.speed
            if (curr.speed > 0) {
                totalSpeed += curr.speed
                speedCount++
            }

            // Heart rate
            curr.heartRate?.let { hr ->
                totalHeartRate += hr
                hrCount++
                if (hr > maxHeartRate) maxHeartRate = hr
            }
        }

        val avgSpeed = if (speedCount > 0) totalSpeed / speedCount else 0f

        // Calculate ski-specific metrics from runs
        val skiDistance = runs.sumOf { it.distance.toDouble() }.toFloat()
        val skiVertical = runs.sumOf { it.verticalDrop.toDouble() }.toFloat()
        val liftDistance = totalDistance - skiDistance
        val avgSkiSpeed = if (runs.isNotEmpty()) {
            runs.map { it.avgSpeed }.average().toFloat()
        } else 0f

        // Time distribution
        val totalDuration = (points.last().timestamp - points.first().timestamp) / 1000
        val (movingTime, stationaryTime, ascendingTime, descendingTime) = calculateTimeDistribution(points)

        // Performance score (0-100)
        val performanceScore = calculatePerformanceScore(runs, avgSkiSpeed, maxSpeed)

        // Speed distribution (6 buckets: 0-10, 10-20, 20-30, 30-40, 40-50, 50+)
        val speedDistribution = calculateSpeedDistribution(points)

        // Heart rate averages
        val avgHeartRate = if (hrCount > 0) totalHeartRate / hrCount else null

        return SessionStats(
            totalDistance = totalDistance,
            skiDistance = skiDistance,
            liftDistance = liftDistance,
            skiVertical = skiVertical,
            totalAscent = totalAscent,
            totalDescent = totalDescent,
            maxAltitude = maxAltitude,
            minAltitude = minAltitude,
            maxSpeed = maxSpeed,
            avgSpeed = avgSpeed,
            avgSkiSpeed = avgSkiSpeed,
            totalDuration = totalDuration,
            movingTime = movingTime,
            stationaryTime = stationaryTime,
            ascendingTime = ascendingTime,
            descendingTime = descendingTime,
            avgHeartRate = avgHeartRate,
            maxHeartRate = if (hrCount > 0) maxHeartRate else null,
            performanceScore = performanceScore,
            speedDistribution = speedDistribution
        )
    }

    /**
     * Calculate time spent in different states
     */
    private fun calculateTimeDistribution(points: List<TrackPoint>): TimeDistribution {
        var movingTime = 0L
        var stationaryTime = 0L
        var ascendingTime = 0L
        var descendingTime = 0L

        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val timeDelta = (curr.timestamp - prev.timestamp) / 1000

            val elevDelta = curr.elevation - prev.elevation
            val elevRate = if (timeDelta > 0) elevDelta / timeDelta else 0.0

            when {
                curr.speed < 3.0f -> stationaryTime += timeDelta
                elevRate > 1.0 -> ascendingTime += timeDelta // Ascending > 1m/s
                elevRate < -1.0 -> descendingTime += timeDelta // Descending < -1m/s
                else -> movingTime += timeDelta
            }
        }

        return TimeDistribution(movingTime, stationaryTime, ascendingTime, descendingTime)
    }

    /**
     * Calculate performance score based on run quality and speed
     */
    private fun calculatePerformanceScore(
        runs: List<com.skigpxrecorder.data.model.SkiRun>,
        avgSkiSpeed: Float,
        maxSpeed: Float
    ): Float {
        if (runs.isEmpty()) return 0f

        // Score components:
        // 1. Number of runs (more = better, capped at 20)
        val runScore = min(runs.size.toFloat() / 20f, 1f) * 25f

        // 2. Average ski speed (0-50 km/h normalized)
        val speedScore = min(avgSkiSpeed / 50f, 1f) * 35f

        // 3. Max speed achievement (0-80 km/h normalized)
        val maxSpeedScore = min(maxSpeed / 80f, 1f) * 20f

        // 4. Vertical meters (more = better, capped at 2000m per run average)
        val avgVertical = runs.map { it.verticalDrop }.average().toFloat()
        val verticalScore = min(avgVertical / 500f, 1f) * 20f

        return runScore + speedScore + maxSpeedScore + verticalScore
    }

    /**
     * Calculate speed distribution histogram
     */
    private fun calculateSpeedDistribution(points: List<TrackPoint>): List<Int> {
        val buckets = IntArray(6) // 0-10, 10-20, 20-30, 30-40, 40-50, 50+

        for (point in points) {
            val bucket = when {
                point.speed < 10f -> 0
                point.speed < 20f -> 1
                point.speed < 30f -> 2
                point.speed < 40f -> 3
                point.speed < 50f -> 4
                else -> 5
            }
            buckets[bucket]++
        }

        return buckets.toList()
    }

    private data class TimeDistribution(
        val movingTime: Long,
        val stationaryTime: Long,
        val ascendingTime: Long,
        val descendingTime: Long
    )
}
