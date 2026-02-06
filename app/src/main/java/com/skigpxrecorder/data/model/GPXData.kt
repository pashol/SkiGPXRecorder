package com.skigpxrecorder.data.model

/**
 * Complete GPX data container with track points, detected runs, and statistics
 */
data class GPXData(
    val trackPoints: List<TrackPoint>,
    val runs: List<SkiRun>,
    val stats: SessionStats,
    val metadata: SessionMetadata,
    val source: DataSource
)

/**
 * Session metadata
 */
data class SessionMetadata(
    val sessionId: String,
    val sessionName: String,
    val startTime: Long,
    val endTime: Long?,
    val isLive: Boolean = false
)

/**
 * Data source for the session
 */
enum class DataSource {
    RECORDED,    // Recorded by this app
    IMPORTED_GPX,
    IMPORTED_FIT
}
