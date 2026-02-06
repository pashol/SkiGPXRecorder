import com.skigpxrecorder.data.model.TrackPoint

// Test data
val points = (0 until 120).map { i ->
    TrackPoint(
        latitude = 0.0 + i * 0.00001,
        longitude = 0.0,
        elevation = 1000.0 - i * 1.0,
        timestamp = i * 1000L,
        speed = 10f,
        accuracy = 10f
    )
}

// Check what smoothed elevations look like
val smoothed = points.map { it.elevation.toFloat() }
println("Points: ${points.size}")
println("First elevation: ${points[0].elevation}")
println("Last elevation: ${points.last().elevation}")
println("Drop: ${points[0].elevation - points.last().elevation}")
