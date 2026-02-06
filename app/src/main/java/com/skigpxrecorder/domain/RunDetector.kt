package com.skigpxrecorder.domain

import com.skigpxrecorder.data.model.SkiRun
import com.skigpxrecorder.data.model.TrackPoint
import com.skigpxrecorder.util.Constants
import kotlin.math.abs

/**
 * Detects ski runs from GPS track points using window-based trend analysis
 *
 * Algorithm (matches Ski-GPX-Analyzer):
 * 1. Smooth elevations and speeds (5-point window)
 * 2. Mark descent points (20-point trend window)
 * 3. Detect raw segments (contiguous descent regions)
 * 4. Combine segments (gap <120s, ascent <50m)
 * 5. Filter (duration ≥60s, vertical ≥30m)
 */
object RunDetector {

    /**
     * Detect ski runs from a complete list of track points (batch mode)
     * Uses full algorithm with segment combination for accurate results
     */
    fun detectRuns(points: List<TrackPoint>): List<SkiRun> {
        if (points.size < Constants.RUN_DETECTION_TREND_WINDOW) return emptyList()

        // 1. Smooth elevations (5-point window)
        val smoothedElevations = StatsCalculator.smoothElevations(points, 5)

        // 2. Smooth speeds (5-point window)
        val smoothedSpeeds = StatsCalculator.smoothSpeeds(points)

        // 3. Mark descent points (window-based, 20-point trend)
        val isDescending = markDescentPoints(points, smoothedElevations, smoothedSpeeds)

        // 4. Detect raw segments (contiguous descent regions)
        val rawSegments = detectRawSegments(points, isDescending)

        // 5. Combine segments (gap <120s, ascent <50m)
        val combinedSegments = combineSegments(rawSegments, points, smoothedElevations)

        // 6. Filter (duration ≥60s, vertical ≥30m)
        val validSegments = combinedSegments.filter { it.isValid(points) }

        // 7. Convert to SkiRun objects
        return validSegments.mapIndexed { index, segment ->
            segment.toSkiRun(index + 1, points, smoothedSpeeds)
        }
    }

    /**
     * Detect runs incrementally (for live recording)
     * Uses simplified algorithm without segment combination for responsive UI
     * Full batch detection runs on session end for accurate results
     */
    fun detectRunsIncremental(points: List<TrackPoint>, previousRuns: List<SkiRun>): List<SkiRun> {
        if (points.size < Constants.RUN_DETECTION_TREND_WINDOW) return emptyList()

        // Simplified: just detect raw segments without combination
        val smoothedElevations = StatsCalculator.smoothElevations(points, 5)
        val smoothedSpeeds = StatsCalculator.smoothSpeeds(points)
        val isDescending = markDescentPoints(points, smoothedElevations, smoothedSpeeds)
        val rawSegments = detectRawSegments(points, isDescending)
        val validSegments = rawSegments.filter { it.isValid(points) }

        return validSegments.mapIndexed { index, segment ->
            segment.toSkiRun(index + 1, points, smoothedSpeeds)
        }
    }

    /**
     * Mark descent points using window-based trend analysis
     * For each point, analyze 20-point window (±10 points)
     * Mark as descending if:
     *   - Speed > 5 km/h
     *   - Elevation drop in window ≥ 10m
     */
    private fun markDescentPoints(
        points: List<TrackPoint>,
        smoothedElevations: List<Float>,
        smoothedSpeeds: List<Float>
    ): BooleanArray {
        val isDescending = BooleanArray(points.size) { false }
        val halfWindow = Constants.RUN_DETECTION_TREND_WINDOW / 2

        for (i in points.indices) {
            // Check speed threshold
            if (smoothedSpeeds[i] < Constants.RUN_DETECTION_SPEED_THRESHOLD) {
                continue
            }

            // Define window bounds
            val windowStart = (i - halfWindow).coerceAtLeast(0)
            val windowEnd = (i + halfWindow).coerceAtMost(points.size - 1)

            // Calculate elevation drop in window
            val elevationAtStart = smoothedElevations[windowStart]
            val elevationAtEnd = smoothedElevations[windowEnd]
            val elevationDrop = elevationAtStart - elevationAtEnd

            // Mark as descending if drop exceeds threshold
            if (elevationDrop >= Constants.RUN_DETECTION_MIN_WINDOW_DROP) {
                isDescending[i] = true
            }
        }

        return isDescending
    }

    /**
     * Detect raw segments (contiguous descent regions)
     */
    private fun detectRawSegments(points: List<TrackPoint>, isDescending: BooleanArray): List<RunSegment> {
        val segments = mutableListOf<RunSegment>()
        var currentSegment: RunSegment? = null

        for (i in points.indices) {
            if (isDescending[i]) {
                if (currentSegment == null) {
                    // Start new segment
                    currentSegment = RunSegment(startIndex = i)
                }
            } else {
                if (currentSegment != null) {
                    // End current segment
                    currentSegment.endIndex = i - 1
                    segments.add(currentSegment)
                    currentSegment = null
                }
            }
        }

        // Close any open segment
        if (currentSegment != null) {
            currentSegment.endIndex = points.size - 1
            segments.add(currentSegment)
        }

        return segments
    }

    /**
     * Combine segments that are close together
     * Merge if:
     *   - Time gap < 120s
     *   - Ascent in gap < 50m
     */
    private fun combineSegments(
        segments: List<RunSegment>,
        points: List<TrackPoint>,
        smoothedElevations: List<Float>
    ): List<RunSegment> {
        if (segments.size <= 1) return segments

        val combined = mutableListOf<RunSegment>()
        var currentSegment = segments[0]

        for (i in 1 until segments.size) {
            val nextSegment = segments[i]

            // Calculate gap between segments
            val gapStartIndex = currentSegment.endIndex
            val gapEndIndex = nextSegment.startIndex
            val gapStartTime = points[gapStartIndex].timestamp
            val gapEndTime = points[gapEndIndex].timestamp
            val gapTime = (gapEndTime - gapStartTime) / 1000 // seconds

            // Calculate ascent in gap
            val gapStartElevation = smoothedElevations[gapStartIndex]
            val gapEndElevation = smoothedElevations[gapEndIndex]
            val ascent = gapEndElevation - gapStartElevation

            // Combine if gap is short and ascent is small
            if (gapTime <= Constants.RUN_DETECTION_MAX_GAP_TIME &&
                ascent <= Constants.RUN_DETECTION_MAX_ASCENT_IN_GAP
            ) {
                // Merge segments
                currentSegment.endIndex = nextSegment.endIndex
            } else {
                // Add current segment and start new one
                combined.add(currentSegment)
                currentSegment = nextSegment
            }
        }

        // Add final segment
        combined.add(currentSegment)

        return combined
    }

    /**
     * Internal class for building runs
     */
    private data class RunSegment(
        val startIndex: Int,
        var endIndex: Int = -1
    ) {
        fun isValid(points: List<TrackPoint>): Boolean {
            if (endIndex == -1 || endIndex < startIndex) return false

            val startPoint = points[startIndex]
            val endPoint = points[endIndex]

            // Check duration threshold
            val duration = (endPoint.timestamp - startPoint.timestamp) / 1000
            if (duration < Constants.RUN_DETECTION_MIN_DURATION) return false

            // Check vertical drop threshold
            val verticalDrop = startPoint.elevation - endPoint.elevation
            if (verticalDrop < Constants.RUN_DETECTION_MIN_VERTICAL) return false

            return true
        }

        fun toSkiRun(runNumber: Int, points: List<TrackPoint>, smoothedSpeeds: List<Float>): SkiRun {
            val runPoints = points.subList(startIndex, endIndex + 1)
            val runSpeeds = smoothedSpeeds.subList(startIndex, endIndex + 1)

            var distance = 0f
            var maxSpeed = 0f
            var totalDistanceWeightedSpeed = 0.0

            for (i in 1 until runPoints.size) {
                val prev = runPoints[i - 1]
                val curr = runPoints[i]
                val segmentDistance = StatsCalculator.calculateDistance(prev, curr).toFloat()
                distance += segmentDistance

                // Use smoothed speed
                val speed = runSpeeds[i]
                if (speed > maxSpeed) {
                    maxSpeed = speed
                }

                // Accumulate distance-weighted speed
                totalDistanceWeightedSpeed += speed * segmentDistance
            }

            // Calculate distance-weighted average speed
            val avgSpeed = if (distance > 0) {
                (totalDistanceWeightedSpeed / distance).toFloat()
            } else {
                0f
            }

            val startPoint = points[startIndex]
            val endPoint = points[endIndex]
            val verticalDrop = (startPoint.elevation - endPoint.elevation).toFloat()
            val avgSlope = if (distance > 0) (verticalDrop / distance) * 100 else 0f

            return SkiRun(
                runNumber = runNumber,
                startIndex = startIndex,
                endIndex = endIndex,
                startTime = startPoint.timestamp,
                endTime = endPoint.timestamp,
                startElevation = startPoint.elevation,
                endElevation = endPoint.elevation,
                maxSpeed = maxSpeed,
                avgSpeed = avgSpeed,
                distance = distance,
                verticalDrop = verticalDrop,
                avgSlope = avgSlope,
                pointCount = runPoints.size
            )
        }
    }
}
