package com.skigpxrecorder.domain

import com.skigpxrecorder.data.model.TrackPoint
import org.junit.Assert.*
import org.junit.Test

class StatsCalculatorTest {

    @Test
    fun `smoothSpeeds with 5-point window reduces spikes`() {
        // Create points with consistent movement and a spike in the middle
        // Movement: ~11 meters per second = ~40 km/h baseline
        val points = listOf(
            createPoint(0.0, 0.0, 0.0, 0L, 40f),
            createPoint(0.0001, 0.0, 10.0, 1000L, 38f),
            createPoint(0.0002, 0.0, 20.0, 2000L, 80f), // spike
            createPoint(0.0003, 0.0, 30.0, 3000L, 42f),
            createPoint(0.0004, 0.0, 40.0, 4000L, 39f),
            createPoint(0.0005, 0.0, 50.0, 5000L, 41f)
        )

        val smoothedSpeeds = StatsCalculator.smoothSpeeds(points)

        assertEquals(6, smoothedSpeeds.size)

        // Smoothed speed at spike point should be much lower than 80 km/h
        assertTrue("Smoothed spike should be < 60 km/h, was ${smoothedSpeeds[2]}",
            smoothedSpeeds[2] < 60f)

        // Smoothed speeds should be more consistent
        val variance = calculateVariance(smoothedSpeeds)
        val rawVariance = calculateVariance(points.map { it.speed })
        assertTrue("Smoothed variance ($variance) should be less than raw variance ($rawVariance)",
            variance < rawVariance)
    }

    @Test
    fun `smoothSpeeds handles edge cases at start and end of track`() {
        val points = listOf(
            createPoint(0.0, 0.0, 0.0, 0L, 10f),
            createPoint(0.0001, 0.0001, 10.0, 1000L, 12f),
            createPoint(0.0002, 0.0002, 20.0, 2000L, 11f)
        )

        val smoothedSpeeds = StatsCalculator.smoothSpeeds(points)

        assertEquals(3, smoothedSpeeds.size)

        // Edge points should still have valid speeds
        assertTrue("First speed should be positive", smoothedSpeeds[0] > 0f)
        assertTrue("Last speed should be positive", smoothedSpeeds[2] > 0f)
    }

    @Test
    fun `smoothSpeeds with single point returns same speed`() {
        val points = listOf(createPoint(0.0, 0.0, 0.0, 0L, 15f))

        val smoothedSpeeds = StatsCalculator.smoothSpeeds(points)

        assertEquals(1, smoothedSpeeds.size)
        assertEquals(15f, smoothedSpeeds[0], 0.01f)
    }

    @Test
    fun `smoothSpeeds with empty list returns empty list`() {
        val smoothedSpeeds = StatsCalculator.smoothSpeeds(emptyList())
        assertTrue(smoothedSpeeds.isEmpty())
    }

    @Test
    fun `calculateStats without dead-zone filtering counts all elevation changes`() {
        // Create points with small elevation changes (<3m)
        val points = listOf(
            createPoint(0.0, 0.0, 100.0, 0L, 10f),
            createPoint(0.0001, 0.0001, 101.5, 1000L, 10f),  // +1.5m gain
            createPoint(0.0002, 0.0002, 103.0, 2000L, 10f),  // +1.5m gain
            createPoint(0.0003, 0.0003, 101.0, 3000L, 10f),  // -2.0m loss
            createPoint(0.0004, 0.0004, 99.0, 4000L, 10f)    // -2.0m loss
        )

        val stats = StatsCalculator.calculateStats(points, 4)

        // All elevation changes should be counted (no 3m dead-zone)
        assertEquals(3.0f, stats.elevationGain, 0.1f)  // 1.5 + 1.5 = 3.0m
        assertEquals(4.0f, stats.elevationLoss, 0.1f)  // 2.0 + 2.0 = 4.0m
    }

    @Test
    fun `calculateStats with empty list returns zero stats`() {
        val stats = StatsCalculator.calculateStats(emptyList(), 0)

        assertEquals(0f, stats.distance, 0.01f)
        assertEquals(0f, stats.elevationGain, 0.01f)
        assertEquals(0f, stats.elevationLoss, 0.01f)
        assertEquals(0f, stats.maxSpeed, 0.01f)
        assertEquals(0f, stats.avgSpeed, 0.01f)
        assertEquals(0, stats.pointCount)
    }

    @Test
    fun `calculateIncrementalStats without dead-zone filtering counts small changes`() {
        val previousStats = StatsCalculator.TrackStats(
            distance = 100f,
            elevationGain = 10f,
            elevationLoss = 5f,
            maxSpeed = 20f,
            avgSpeed = 15f,
            pointCount = 5,
            currentSpeed = 12f,
            currentElevation = 100f,
            currentAccuracy = 10f
        )

        val previousPoint = createPoint(0.0, 0.0, 100.0, 5000L, 12f)
        val newPoint = createPoint(0.0001, 0.0001, 101.5, 6000L, 13f) // +1.5m gain (< 3m)

        val newStats = StatsCalculator.calculateIncrementalStats(
            previousStats,
            newPoint,
            previousPoint,
            6
        )

        // Should count the 1.5m gain (no dead-zone filtering)
        assertEquals(11.5f, newStats.elevationGain, 0.1f)
        assertEquals(5f, newStats.elevationLoss, 0.1f)
    }

    @Test
    fun `smoothElevations with 5-point window reduces noise`() {
        // Create points with noisy elevation data
        val points = listOf(
            createPoint(0.0, 0.0, 100.0, 0L, 10f),
            createPoint(0.0001, 0.0001, 102.0, 1000L, 10f),
            createPoint(0.0002, 0.0002, 99.0, 2000L, 10f),  // noise
            createPoint(0.0003, 0.0003, 101.0, 3000L, 10f),
            createPoint(0.0004, 0.0004, 103.0, 4000L, 10f)
        )

        val smoothedElevations = StatsCalculator.smoothElevations(points, 5)

        assertEquals(5, smoothedElevations.size)

        // Smoothed elevations should have less variance
        val variance = calculateVariance(smoothedElevations)
        val rawVariance = calculateVariance(points.map { it.elevation.toFloat() })
        assertTrue("Smoothed variance should be less than raw variance", variance < rawVariance)
    }

    // Helper functions

    private fun createPoint(
        lat: Double,
        lon: Double,
        elevation: Double,
        timestamp: Long,
        speed: Float,
        accuracy: Float = 10f
    ): TrackPoint {
        return TrackPoint(
            latitude = lat,
            longitude = lon,
            elevation = elevation,
            timestamp = timestamp,
            speed = speed,
            accuracy = accuracy
        )
    }

    private fun calculateVariance(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        val mean = values.average().toFloat()
        val squaredDiffs = values.map { (it - mean) * (it - mean) }
        return squaredDiffs.average().toFloat()
    }
}
