package com.skigpxrecorder.domain

import com.skigpxrecorder.data.model.TrackPoint
import com.skigpxrecorder.util.Constants
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object StatsCalculator {

    private const val EARTH_RADIUS = 6371000.0 // meters

    data class TrackStats(
        val distance: Float = 0f,              // meters
        val elevationGain: Float = 0f,         // meters
        val elevationLoss: Float = 0f,         // meters
        val maxSpeed: Float = 0f,              // km/h
        val avgSpeed: Float = 0f,              // km/h
        val pointCount: Int = 0,
        val currentSpeed: Float = 0f,          // km/h
        val currentElevation: Float = 0f,      // meters
        val currentAccuracy: Float = 0f,       // meters
        val skiDistance: Float = 0f,           // meters - distance during runs only
        val skiVertical: Float = 0f,           // meters - total elevation drop from all runs
        val avgSkiSpeed: Float = 0f            // km/h - distance-weighted average speed during runs
    )

    fun calculateStats(
        points: List<TrackPoint>,
        elapsedTimeSeconds: Long
    ): TrackStats {
        if (points.isEmpty()) return TrackStats()

        val currentPoint = points.last()
        var distance = 0f
        var elevationGain = 0f
        var elevationLoss = 0f
        var maxSpeed = 0f

        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]

            // Calculate distance
            distance += calculateDistance(prev, curr).toFloat()

            // Calculate elevation change (no dead-zone filtering)
            val elevationDiff = curr.elevation - prev.elevation
            if (elevationDiff > 0) {
                elevationGain += elevationDiff.toFloat()
            } else {
                elevationLoss += kotlin.math.abs(elevationDiff).toFloat()
            }

            // Track max speed
            if (curr.speed > maxSpeed) {
                maxSpeed = curr.speed
            }
        }

        // Calculate average speed
        val avgSpeed = if (elapsedTimeSeconds > 0) {
            (distance / elapsedTimeSeconds) * 3.6f // m/s to km/h
        } else {
            0f
        }

        return TrackStats(
            distance = distance,
            elevationGain = elevationGain,
            elevationLoss = elevationLoss,
            maxSpeed = maxSpeed,
            avgSpeed = avgSpeed,
            pointCount = points.size,
            currentSpeed = currentPoint.speed,
            currentElevation = currentPoint.elevation.toFloat(),
            currentAccuracy = currentPoint.accuracy
        )
    }

    fun calculateIncrementalStats(
        previousStats: TrackStats,
        newPoint: TrackPoint,
        previousPoint: TrackPoint?,
        elapsedTimeSeconds: Long
    ): TrackStats {
        if (previousPoint == null) {
            return TrackStats(
                pointCount = 1,
                currentSpeed = newPoint.speed,
                currentElevation = newPoint.elevation.toFloat(),
                currentAccuracy = newPoint.accuracy
            )
        }

        val distance = previousStats.distance + calculateDistance(previousPoint, newPoint).toFloat()

        val elevationDiff = newPoint.elevation - previousPoint.elevation
        val elevationGain: Float
        val elevationLoss: Float

        // Calculate elevation change (no dead-zone filtering)
        if (elevationDiff > 0) {
            elevationGain = previousStats.elevationGain + elevationDiff.toFloat()
            elevationLoss = previousStats.elevationLoss
        } else {
            elevationGain = previousStats.elevationGain
            elevationLoss = previousStats.elevationLoss + kotlin.math.abs(elevationDiff).toFloat()
        }

        val maxSpeed = kotlin.math.max(previousStats.maxSpeed, newPoint.speed)
        
        val avgSpeed = if (elapsedTimeSeconds > 0) {
            (distance / elapsedTimeSeconds) * 3.6f
        } else {
            0f
        }

        return TrackStats(
            distance = distance,
            elevationGain = elevationGain,
            elevationLoss = elevationLoss,
            maxSpeed = maxSpeed,
            avgSpeed = avgSpeed,
            pointCount = previousStats.pointCount + 1,
            currentSpeed = newPoint.speed,
            currentElevation = newPoint.elevation.toFloat(),
            currentAccuracy = newPoint.accuracy
        )
    }

    /**
     * Calculate distance between two points using Haversine formula
     */
    fun calculateDistance(p1: TrackPoint, p2: TrackPoint): Double {
        val lat1Rad = Math.toRadians(p1.latitude)
        val lat2Rad = Math.toRadians(p2.latitude)
        val deltaLat = Math.toRadians(p2.latitude - p1.latitude)
        val deltaLon = Math.toRadians(p2.longitude - p1.longitude)

        val a = sin(deltaLat / 2).pow(2.0) +
                cos(lat1Rad) * cos(lat2Rad) * sin(deltaLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS * c
    }

    /**
     * Calculate slope percentage between two points
     * Returns slope as percentage (e.g., 25.0 for 25%)
     */
    fun calculateSlope(p1: TrackPoint, p2: TrackPoint): Float {
        val horizontalDistance = calculateDistance(p1, p2)
        if (horizontalDistance == 0.0) return 0f

        val elevationChange = p2.elevation - p1.elevation
        return ((elevationChange / horizontalDistance) * 100).toFloat()
    }

    /**
     * Smooth speeds using a moving window approach
     * For each point, calculate distance/time over a window of points
     * Returns a list of smoothed speeds (same length as input)
     */
    fun smoothSpeeds(points: List<TrackPoint>): List<Float> {
        if (points.isEmpty()) return emptyList()
        if (points.size == 1) return listOf(points[0].speed)

        val windowSize = Constants.SPEED_SMOOTHING_WINDOW
        val smoothedSpeeds = mutableListOf<Float>()

        for (i in points.indices) {
            // Define window bounds
            val windowStart = (i - windowSize / 2).coerceAtLeast(0)
            val windowEnd = (i + windowSize / 2 + 1).coerceAtMost(points.size)

            if (windowEnd - windowStart <= 1) {
                // Not enough points in window, use raw speed
                smoothedSpeeds.add(points[i].speed)
                continue
            }

            // Calculate distance and time over window
            var windowDistance = 0.0
            var windowTime = 0L

            for (j in windowStart until windowEnd - 1) {
                val curr = points[j]
                val next = points[j + 1]
                windowDistance += calculateDistance(curr, next)
                windowTime += (next.timestamp - curr.timestamp)
            }

            // Calculate smoothed speed (distance/time)
            val smoothedSpeed = if (windowTime > 0) {
                ((windowDistance / (windowTime / 1000.0)) * 3.6).toFloat() // m/s to km/h
            } else {
                points[i].speed
            }

            smoothedSpeeds.add(smoothedSpeed)
        }

        return smoothedSpeeds
    }

    /**
     * Smooth elevations using a moving average
     * Returns a list of smoothed elevations (same length as input)
     */
    fun smoothElevations(points: List<TrackPoint>, windowSize: Int = Constants.SPEED_SMOOTHING_WINDOW): List<Float> {
        if (points.isEmpty()) return emptyList()
        if (points.size == 1) return listOf(points[0].elevation.toFloat())

        val smoothedElevations = mutableListOf<Float>()

        for (i in points.indices) {
            // Define window bounds
            val windowStart = (i - windowSize / 2).coerceAtLeast(0)
            val windowEnd = (i + windowSize / 2 + 1).coerceAtMost(points.size)

            // Calculate average elevation in window
            val windowElevations = points.subList(windowStart, windowEnd).map { it.elevation }
            val avgElevation = windowElevations.average().toFloat()

            smoothedElevations.add(avgElevation)
        }

        return smoothedElevations
    }
}
