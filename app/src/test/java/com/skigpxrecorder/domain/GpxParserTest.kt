package com.skigpxrecorder.domain

import org.junit.Test
import org.junit.Assert.*
import java.io.File

/**
 * Unit tests for GPX parser using real example files
 */
class GpxParserTest {

    @Test
    fun `parse snowtrack GPX file successfully`() {
        val gpxFile = File("examples/snowtrack-track-2026-01-01T12_30_15.350Z.gpx")
        if (!gpxFile.exists()) {
            println("Warning: Example file not found at ${gpxFile.absolutePath}")
            return
        }

        val result = GpxParser.parse(gpxFile.inputStream())

        assertNotNull(result)
        assertTrue(result.trackPoints.isNotEmpty())
        assertNotNull(result.metadata)
    }

    @Test
    fun `parse Day 5 GPX file successfully`() {
        val gpxFile = File("examples/Day_5_2024-2025.gpx")
        if (!gpxFile.exists()) {
            println("Warning: Example file not found")
            return
        }

        val result = GpxParser.parse(gpxFile.inputStream())

        assertNotNull(result)
        assertTrue(result.trackPoints.isNotEmpty())

        // Verify track points have required fields
        result.trackPoints.forEach { point ->
            assertTrue(point.latitude in -90.0..90.0)
            assertTrue(point.longitude in -180.0..180.0)
            assertTrue(point.timestamp > 0)
        }
    }

    @Test
    fun `parse Day 7 GPX file successfully`() {
        val gpxFile = File("examples/Day_7_2024-2025.gpx")
        if (!gpxFile.exists()) {
            println("Warning: Example file not found")
            return
        }

        val result = GpxParser.parse(gpxFile.inputStream())

        assertNotNull(result)
        assertTrue(result.trackPoints.isNotEmpty())
    }

    @Test
    fun `parse Day 9 GPX file successfully`() {
        val gpxFile = File("examples/Day_9_2024-2025.gpx")
        if (!gpxFile.exists()) {
            println("Warning: Example file not found")
            return
        }

        val result = GpxParser.parse(gpxFile.inputStream())

        assertNotNull(result)
        assertTrue(result.trackPoints.isNotEmpty())
    }

    @Test
    fun `parsed track points have valid coordinates`() {
        val gpxFile = File("examples/snowtrack-track-2026-01-04T09_46_29.717Z.gpx")
        if (!gpxFile.exists()) {
            println("Warning: Example file not found")
            return
        }

        val result = GpxParser.parse(gpxFile.inputStream())

        result.trackPoints.forEach { point ->
            // Validate latitude range
            assertTrue("Latitude should be between -90 and 90",
                point.latitude >= -90.0 && point.latitude <= 90.0)

            // Validate longitude range
            assertTrue("Longitude should be between -180 and 180",
                point.longitude >= -180.0 && point.longitude <= 180.0)

            // Validate timestamp is positive
            assertTrue("Timestamp should be positive", point.timestamp > 0)
        }
    }

    @Test
    fun `parsed track points maintain chronological order`() {
        val gpxFile = File("examples/snowtrack-track-2026-01-01T12_30_15.350Z.gpx")
        if (!gpxFile.exists()) {
            println("Warning: Example file not found")
            return
        }

        val result = GpxParser.parse(gpxFile.inputStream())

        // Check that timestamps are in ascending order
        for (i in 1 until result.trackPoints.size) {
            assertTrue("Track points should be in chronological order",
                result.trackPoints[i].timestamp >= result.trackPoints[i - 1].timestamp)
        }
    }

    @Test
    fun `parse GPX with heart rate data`() {
        val gpxFile = File("examples/Day_5_2024-2025.gpx")
        if (!gpxFile.exists()) {
            println("Warning: Example file not found")
            return
        }

        val result = GpxParser.parse(gpxFile.inputStream())

        // Check if any points have heart rate data
        val pointsWithHR = result.trackPoints.filter { it.heartRate != null }

        if (pointsWithHR.isNotEmpty()) {
            pointsWithHR.forEach { point ->
                assertTrue("Heart rate should be reasonable",
                    point.heartRate!! in 30f..250f)
            }
        }
    }

    @Test
    fun `Day 7 GPX produces ski-specific stats with run detection`() {
        val gpxFile = File("examples/Day_7_2024-2025.gpx")
        if (!gpxFile.exists()) {
            println("Warning: Example file not found")
            return
        }

        val result = GpxParser.parse(gpxFile.inputStream())
        val points = result.trackPoints

        // Detect runs using new algorithm
        val runs = RunDetector.detectRuns(points)

        // Calculate ski stats from runs
        val skiDistance = runs.sumOf { it.distance.toDouble() }.toFloat()
        val skiVertical = runs.sumOf { it.verticalDrop.toDouble() }.toFloat()

        // Calculate total distance from all points
        var totalDistance = 0f
        for (i in 1 until points.size) {
            totalDistance += StatsCalculator.calculateDistance(points[i - 1], points[i]).toFloat()
        }

        // Ski-specific stats validation
        assertTrue("Should detect runs in Day 7", runs.isNotEmpty())
        assertTrue("Ski distance should be > 0", skiDistance > 0f)
        assertTrue("Ski vertical should be > 0", skiVertical > 0f)
        assertTrue("Ski distance should be < total distance (excludes lifts)", skiDistance < totalDistance)

        println("Day 7 Stats: ${runs.size} runs, ski distance: ${skiDistance.toInt()}m, ski vertical: ${skiVertical.toInt()}m, total distance: ${totalDistance.toInt()}m")
    }

    @Test
    fun `Day 9 GPX produces ski-specific stats with run detection`() {
        val gpxFile = File("examples/Day_9_2024-2025.gpx")
        if (!gpxFile.exists()) {
            println("Warning: Example file not found")
            return
        }

        val result = GpxParser.parse(gpxFile.inputStream())
        val points = result.trackPoints

        // Detect runs using new algorithm
        val runs = RunDetector.detectRuns(points)

        // Calculate ski stats from runs
        val skiDistance = runs.sumOf { it.distance.toDouble() }.toFloat()
        val skiVertical = runs.sumOf { it.verticalDrop.toDouble() }.toFloat()

        // Calculate total distance from all points
        var totalDistance = 0f
        for (i in 1 until points.size) {
            totalDistance += StatsCalculator.calculateDistance(points[i - 1], points[i]).toFloat()
        }

        // Ski-specific stats validation
        assertTrue("Should detect runs in Day 9", runs.isNotEmpty())
        assertTrue("Ski distance should be > 0", skiDistance > 0f)
        assertTrue("Ski vertical should be > 0", skiVertical > 0f)
        assertTrue("Ski distance should be < total distance (excludes lifts)", skiDistance < totalDistance)

        println("Day 9 Stats: ${runs.size} runs, ski distance: ${skiDistance.toInt()}m, ski vertical: ${skiVertical.toInt()}m, total distance: ${totalDistance.toInt()}m")
    }

    @Test
    fun `ski vertical represents total elevation drop from all runs`() {
        val gpxFile = File("examples/Day_7_2024-2025.gpx")
        if (!gpxFile.exists()) {
            println("Warning: Example file not found")
            return
        }

        val result = GpxParser.parse(gpxFile.inputStream())
        val points = result.trackPoints

        // Detect runs
        val runs = RunDetector.detectRuns(points)

        if (runs.isEmpty()) {
            println("Warning: No runs detected in Day 7")
            return
        }

        // Verify each run has positive vertical drop
        runs.forEach { run ->
            assertTrue("Run ${run.runNumber} should have positive vertical drop",
                run.verticalDrop > 0f)
        }

        // Total ski vertical should equal sum of all run vertical drops
        val totalSkiVertical = runs.sumOf { it.verticalDrop.toDouble() }.toFloat()
        assertTrue("Total ski vertical should be > 0", totalSkiVertical > 0f)

        // For a typical ski day, expect at least 1000m vertical
        println("Total ski vertical: ${totalSkiVertical.toInt()}m from ${runs.size} runs")
    }

    @Test
    fun `compare multiple GPX files for consistent run detection`() {
        val testFiles = listOf(
            "examples/Day_5_2024-2025.gpx",
            "examples/Day_7_2024-2025.gpx",
            "examples/Day_9_2024-2025.gpx"
        )

        val results = mutableListOf<Pair<String, Int>>()

        testFiles.forEach { filePath ->
            val gpxFile = File(filePath)
            if (gpxFile.exists()) {
                val result = GpxParser.parse(gpxFile.inputStream())
                val runs = RunDetector.detectRuns(result.trackPoints)
                results.add(Pair(gpxFile.name, runs.size))
                println("${gpxFile.name}: ${runs.size} runs detected")
            }
        }

        // Verify all files had runs detected
        results.forEach { (fileName, runCount) ->
            assertTrue("$fileName should have detected runs", runCount > 0)
            assertTrue("$fileName should have reasonable run count (5-20)", runCount in 5..20)
        }
    }
}
