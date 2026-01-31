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
        val currentAccuracy: Float = 0f        // meters
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

            // Calculate elevation change with dead-zone filtering
            val elevationDiff = curr.elevation - prev.elevation
            if (kotlin.math.abs(elevationDiff) > Constants.ELEVATION_CHANGE_THRESHOLD) {
                if (elevationDiff > 0) {
                    elevationGain += elevationDiff.toFloat()
                } else {
                    elevationLoss += kotlin.math.abs(elevationDiff).toFloat()
                }
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

        if (kotlin.math.abs(elevationDiff) > Constants.ELEVATION_CHANGE_THRESHOLD) {
            if (elevationDiff > 0) {
                elevationGain = previousStats.elevationGain + elevationDiff.toFloat()
                elevationLoss = previousStats.elevationLoss
            } else {
                elevationGain = previousStats.elevationGain
                elevationLoss = previousStats.elevationLoss + kotlin.math.abs(elevationDiff).toFloat()
            }
        } else {
            elevationGain = previousStats.elevationGain
            elevationLoss = previousStats.elevationLoss
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
    private fun calculateDistance(p1: TrackPoint, p2: TrackPoint): Double {
        val lat1Rad = Math.toRadians(p1.latitude)
        val lat2Rad = Math.toRadians(p2.latitude)
        val deltaLat = Math.toRadians(p2.latitude - p1.latitude)
        val deltaLon = Math.toRadians(p2.longitude - p1.longitude)

        val a = sin(deltaLat / 2).pow(2.0) +
                cos(lat1Rad) * cos(lat2Rad) * sin(deltaLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS * c
    }
}
