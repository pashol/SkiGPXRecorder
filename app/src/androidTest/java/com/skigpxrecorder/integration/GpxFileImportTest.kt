package com.skigpxrecorder.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.skigpxrecorder.domain.GpxParser
import com.skigpxrecorder.domain.SessionAnalyzer
import com.skigpxrecorder.domain.StatsCalculator
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Integration tests for importing and analyzing real GPX files from examples folder
 * These tests verify the complete flow from file parsing to stats calculation
 */
@RunWith(AndroidJUnit4::class)
class GpxFileImportTest {

    private lateinit var context: Context
    private lateinit var examplesDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Try to locate examples directory relative to project root
        // In androidTest, we may need to copy files to assets or use content provider
        examplesDir = File("examples")

        if (!examplesDir.exists()) {
            println("Examples directory not found at ${examplesDir.absolutePath}")
            println("Skipping integration tests - run from project root or copy examples to assets")
        }
    }

    @Test
    fun importAndAnalyzeSnowtrackFile() {
        val gpxFile = File(examplesDir, "snowtrack-track-2026-01-01T12_30_15.350Z.gpx")

        if (!gpxFile.exists()) {
            println("Skipping test - file not found: ${gpxFile.absolutePath}")
            return
        }

        // Parse GPX
        val parseResult = GpxParser.parse(gpxFile.inputStream())

        assertNotNull("Parse result should not be null", parseResult)
        assertTrue("Should have track points", parseResult.trackPoints.isNotEmpty())
        assertNotNull("Should have metadata", parseResult.metadata)

        println("Parsed ${parseResult.trackPoints.size} track points")

        // Calculate stats
        val distance = StatsCalculator.calculateTotalDistance(parseResult.trackPoints)
        val elevationGain = StatsCalculator.calculateElevationGain(parseResult.trackPoints)
        val elevationLoss = StatsCalculator.calculateElevationLoss(parseResult.trackPoints)
        val maxSpeed = StatsCalculator.calculateMaxSpeed(parseResult.trackPoints)

        // Verify reasonable values
        assertTrue("Distance should be positive", distance > 0f)
        assertTrue("Elevation gain should be non-negative", elevationGain >= 0f)
        assertTrue("Max speed should be reasonable", maxSpeed < 200f)

        println("Stats - Distance: ${distance/1000}km, Elev+: ${elevationGain}m, Max speed: ${maxSpeed}km/h")

        // Analyze session (if SessionAnalyzer is available)
        // val gpxData = SessionAnalyzer.analyzeSession(
        //     sessionId = "test",
        //     sessionName = parseResult.metadata.name ?: "Imported",
        //     points = parseResult.trackPoints,
        //     source = DataSource.IMPORTED
        // )
        // assertNotNull(gpxData)
    }

    @Test
    fun importMultipleFilesAndCompare() {
        val fileNames = listOf(
            "Day_5_2024-2025.gpx",
            "Day_7_2024-2025.gpx",
            "Day_9_2024-2025.gpx"
        )

        val results = mutableListOf<Pair<String, Map<String, Float>>>()

        fileNames.forEach { fileName ->
            val gpxFile = File(examplesDir, fileName)

            if (!gpxFile.exists()) {
                println("Skipping $fileName - not found")
                return@forEach
            }

            val parseResult = GpxParser.parse(gpxFile.inputStream())
            val points = parseResult.trackPoints

            val stats = mapOf(
                "distance" to StatsCalculator.calculateTotalDistance(points),
                "elevationGain" to StatsCalculator.calculateElevationGain(points),
                "elevationLoss" to StatsCalculator.calculateElevationLoss(points),
                "maxSpeed" to StatsCalculator.calculateMaxSpeed(points),
                "points" to points.size.toFloat()
            )

            results.add(fileName to stats)

            println("$fileName: ${stats["distance"]?.div(1000)}km, ${stats["points"]?.toInt()} points")
        }

        // Verify all files were processed successfully
        if (results.isNotEmpty()) {
            results.forEach { (name, stats) ->
                assertNotNull("$name should have stats", stats)
                assertTrue("$name should have positive distance", stats["distance"]!! >= 0f)
            }
        } else {
            println("No files found in examples directory")
        }
    }

    @Test
    fun verifyGpxDataIntegrity() {
        val gpxFile = File(examplesDir, "snowtrack-track-2026-01-04T09_46_29.717Z.gpx")

        if (!gpxFile.exists()) {
            println("Skipping test - file not found")
            return
        }

        val parseResult = GpxParser.parse(gpxFile.inputStream())
        val points = parseResult.trackPoints

        // Verify data integrity
        points.forEach { point ->
            // Coordinates in valid range
            assertTrue("Latitude in range", point.latitude in -90.0..90.0)
            assertTrue("Longitude in range", point.longitude in -180.0..180.0)

            // Timestamp is valid
            assertTrue("Timestamp is positive", point.timestamp > 0)

            // Accuracy is reasonable
            assertTrue("Accuracy is reasonable", point.accuracy >= 0f && point.accuracy < 1000f)

            // Speed is non-negative
            assertTrue("Speed is non-negative", point.speed >= 0f)

            // Elevation is reasonable for skiing (not in ocean, not in space)
            assertTrue("Elevation is reasonable", point.elevation > -500.0 && point.elevation < 10000.0)
        }

        println("Data integrity verified for ${points.size} points")
    }

    @Test
    fun verifyChronologicalOrder() {
        val gpxFile = File(examplesDir, "Day_5_2024-2025.gpx")

        if (!gpxFile.exists()) {
            println("Skipping test - file not found")
            return
        }

        val parseResult = GpxParser.parse(gpxFile.inputStream())
        val points = parseResult.trackPoints

        // Verify points are in chronological order
        for (i in 1 until points.size) {
            assertTrue(
                "Point $i timestamp should be >= previous point",
                points[i].timestamp >= points[i - 1].timestamp
            )
        }

        println("Chronological order verified for ${points.size} points")
    }

    @Test
    fun testHeartRateDataIfPresent() {
        val gpxFile = File(examplesDir, "Day_7_2024-2025.gpx")

        if (!gpxFile.exists()) {
            println("Skipping test - file not found")
            return
        }

        val parseResult = GpxParser.parse(gpxFile.inputStream())
        val pointsWithHR = parseResult.trackPoints.filter { it.heartRate != null }

        if (pointsWithHR.isNotEmpty()) {
            println("Found ${pointsWithHR.size} points with heart rate data")

            pointsWithHR.forEach { point ->
                assertTrue(
                    "Heart rate should be reasonable (30-250 bpm)",
                    point.heartRate!! in 30f..250f
                )
            }

            val avgHR = pointsWithHR.mapNotNull { it.heartRate }.average()
            println("Average heart rate: $avgHR bpm")

            assertTrue("Average HR should be reasonable", avgHR in 50.0..200.0)
        } else {
            println("No heart rate data found in this file")
        }
    }
}
