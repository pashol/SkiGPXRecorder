package com.skigpxrecorder.domain

import com.skigpxrecorder.data.model.TrackPoint
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object GpxWriter {

    fun generateGpx(points: List<TrackPoint>, trackName: String): String {
        if (points.isEmpty()) return ""

        return buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<gpx version="1.1" creator="Ski GPX Recorder" xmlns="http://www.topografix.com/GPX/1/1">""")
            appendLine("  <metadata>")
            appendLine("    <name>$trackName</name>")
            appendLine("    <time>${formatIsoTime(points.first().timestamp)}</time>")
            appendLine("  </metadata>")
            appendLine("  <trk>")
            appendLine("    <name>$trackName</name>")
            appendLine("    <trkseg>")

            points.forEach { point ->
                appendLine("""      <trkpt lat="${point.latitude}" lon="${point.longitude}">""")
                appendLine("""        <ele>${point.elevation}</ele>""")
                appendLine("""        <time>${formatIsoTime(point.timestamp)}</time>""")
                // Include accuracy in extensions for post-processing filtering
                if (point.accuracy > 0f) {
                    appendLine("""        <extensions>""")
                    appendLine("""          <accuracy>${point.accuracy}</accuracy>""")
                    appendLine("""        </extensions>""")
                }
                appendLine("""      </trkpt>""")
            }

            appendLine("    </trkseg>")
            appendLine("  </trk>")
            appendLine("</gpx>")
        }
    }

    private fun formatIsoTime(timestamp: Long): String {
        val instant = Instant.ofEpochMilli(timestamp)
        return DateTimeFormatter.ISO_INSTANT.format(instant)
    }

    fun generateTrackName(): String {
        val now = java.time.LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm", Locale.getDefault())
        return "Ski_Track_${now.format(formatter)}"
    }
}
