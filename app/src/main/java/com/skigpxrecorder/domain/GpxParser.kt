package com.skigpxrecorder.domain

import android.util.Xml
import com.skigpxrecorder.data.model.TrackPoint
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.time.Instant

/**
 * XML pull parser for GPX files
 * Extracts track points with lat, lon, ele, time, and extensions (HR)
 */
object GpxParser {

    data class GpxMetadata(
        val name: String? = null,
        val time: String? = null
    )

    data class ParseResult(
        val trackPoints: List<TrackPoint>,
        val metadata: GpxMetadata
    )

    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(inputStream: InputStream): ParseResult {
        inputStream.use { stream ->
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(stream, null)
            parser.nextTag()
            val result = readGpx(parser)

            // Calculate speeds from position/time deltas for imported GPX files
            val pointsWithSpeed = calculateSpeeds(result.trackPoints)
            return result.copy(trackPoints = pointsWithSpeed)
        }
    }

    /**
     * Calculate speed for each point based on distance/time deltas
     */
    private fun calculateSpeeds(points: List<TrackPoint>): List<TrackPoint> {
        if (points.size < 2) return points

        val result = mutableListOf<TrackPoint>()

        // First point has zero speed
        result.add(points[0])

        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]

            val timeDelta = (curr.timestamp - prev.timestamp) / 1000.0 // seconds
            val distance = StatsCalculator.calculateDistance(prev, curr) // meters

            // Calculate speed in km/h
            val speed = if (timeDelta > 0) {
                ((distance / timeDelta) * 3.6).toFloat() // m/s to km/h
            } else {
                0f
            }

            result.add(curr.copy(speed = speed))
        }

        return result
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readGpx(parser: XmlPullParser): ParseResult {
        val trackPoints = mutableListOf<TrackPoint>()
        var metadata = GpxMetadata()

        parser.require(XmlPullParser.START_TAG, null, "gpx")

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }

            when (parser.name) {
                "metadata" -> metadata = readMetadata(parser)
                "trk" -> trackPoints.addAll(readTrack(parser))
                else -> skip(parser)
            }
        }

        return ParseResult(trackPoints, metadata)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readMetadata(parser: XmlPullParser): GpxMetadata {
        parser.require(XmlPullParser.START_TAG, null, "metadata")
        var name: String? = null
        var time: String? = null

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }

            when (parser.name) {
                "name" -> name = readText(parser, "name")
                "time" -> time = readText(parser, "time")
                else -> skip(parser)
            }
        }

        return GpxMetadata(name, time)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readTrack(parser: XmlPullParser): List<TrackPoint> {
        val trackPoints = mutableListOf<TrackPoint>()
        parser.require(XmlPullParser.START_TAG, null, "trk")

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }

            when (parser.name) {
                "trkseg" -> trackPoints.addAll(readTrackSegment(parser))
                else -> skip(parser)
            }
        }

        return trackPoints
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readTrackSegment(parser: XmlPullParser): List<TrackPoint> {
        val trackPoints = mutableListOf<TrackPoint>()
        parser.require(XmlPullParser.START_TAG, null, "trkseg")

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }

            when (parser.name) {
                "trkpt" -> {
                    trackPoints.add(readTrackPoint(parser))
                }
                else -> skip(parser)
            }
        }

        return trackPoints
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readTrackPoint(parser: XmlPullParser): TrackPoint {
        parser.require(XmlPullParser.START_TAG, null, "trkpt")

        val lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull() ?: 0.0
        val lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull() ?: 0.0

        var elevation = 0.0
        var time = System.currentTimeMillis()
        var heartRate: Float? = null
        var speed = 0f

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }

            when (parser.name) {
                "ele" -> elevation = readText(parser, "ele").toDoubleOrNull() ?: 0.0
                "time" -> {
                    val timeStr = readText(parser, "time")
                    time = try {
                        Instant.parse(timeStr).toEpochMilli()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }
                }
                "extensions" -> {
                    heartRate = readExtensions(parser)
                }
                else -> skip(parser)
            }
        }

        return TrackPoint(
            latitude = lat,
            longitude = lon,
            elevation = elevation,
            timestamp = time,
            speed = speed,
            accuracy = 5f, // Default good accuracy for imported files
            heartRate = heartRate
        )
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readExtensions(parser: XmlPullParser): Float? {
        parser.require(XmlPullParser.START_TAG, null, "extensions")
        var heartRate: Float? = null

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }

            // Look for heart rate in various formats
            when {
                parser.name.contains("hr", ignoreCase = true) ||
                parser.name.contains("heartrate", ignoreCase = true) -> {
                    heartRate = readText(parser, parser.name).toFloatOrNull()
                }
                else -> skip(parser)
            }
        }

        return heartRate
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readText(parser: XmlPullParser, tag: String): String {
        parser.require(XmlPullParser.START_TAG, null, tag)
        var text = ""
        if (parser.next() == XmlPullParser.TEXT) {
            text = parser.text
            parser.nextTag()
        }
        parser.require(XmlPullParser.END_TAG, null, tag)
        return text
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }
}
