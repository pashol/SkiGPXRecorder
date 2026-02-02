package com.skigpxrecorder.data.repository

import android.content.Context
import com.skigpxrecorder.data.local.SessionDao
import com.skigpxrecorder.data.local.SkiRunDao
import com.skigpxrecorder.data.local.TrackPointDao
import com.skigpxrecorder.data.model.DataSource
import com.skigpxrecorder.data.model.GPXData
import com.skigpxrecorder.data.model.RecordingSession
import com.skigpxrecorder.data.model.SkiRun
import com.skigpxrecorder.data.model.TrackPoint
import com.skigpxrecorder.domain.GpxWriter
import com.skigpxrecorder.domain.RunDetector
import com.skigpxrecorder.domain.SessionAnalyzer
import com.skigpxrecorder.domain.StatsCalculator
import com.skigpxrecorder.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GpxRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionDao: SessionDao,
    private val trackPointDao: TrackPointDao,
    private val skiRunDao: SkiRunDao,
    private val runDetector: RunDetector
) {
    private val trackPoints = mutableListOf<TrackPoint>()
    private var currentSession: RecordingSession? = null
    private var lastSavedIndex = 0

    private val _currentStats = MutableStateFlow(StatsCalculator.TrackStats())
    val currentStats: StateFlow<StatsCalculator.TrackStats> = _currentStats.asStateFlow()

    private val _currentRuns = MutableStateFlow<List<SkiRun>>(emptyList())
    val currentRuns: StateFlow<List<SkiRun>> = _currentRuns.asStateFlow()

    private var startTime: Long = 0
    private var lastRunDetectionTime: Long = 0

    fun addTrackPoint(point: TrackPoint) {
        synchronized(trackPoints) {
            val previousPoint = trackPoints.lastOrNull()
            trackPoints.add(point)
            android.util.Log.i("GpxRepository", "Track point added. Total points: ${trackPoints.size}")

            val elapsedTime = (System.currentTimeMillis() - startTime) / 1000
            val newStats = StatsCalculator.calculateIncrementalStats(
                _currentStats.value,
                point,
                previousPoint,
                elapsedTime
            )

            // Run detection (throttled - every 5 seconds)
            val now = System.currentTimeMillis()
            if (now - lastRunDetectionTime > 5000 && trackPoints.size > 10) {
                lastRunDetectionTime = now
                val detectedRuns = runDetector.detectRunsIncremental(
                    trackPoints.toList(),
                    _currentRuns.value
                )
                _currentRuns.value = detectedRuns
                android.util.Log.i("GpxRepository", "Runs detected: ${detectedRuns.size}")

                // Calculate ski stats from detected runs
                val (skiDistance, skiVertical, avgSkiSpeed) = calculateSkiStats(detectedRuns)
                _currentStats.value = newStats.copy(
                    skiDistance = skiDistance,
                    skiVertical = skiVertical,
                    avgSkiSpeed = avgSkiSpeed
                )
            } else {
                _currentStats.value = newStats
            }
        }
    }

    /**
     * Calculate ski-specific statistics from detected runs
     * Returns Triple of (skiDistance, skiVertical, avgSkiSpeed)
     */
    private fun calculateSkiStats(runs: List<SkiRun>): Triple<Float, Float, Float> {
        if (runs.isEmpty()) return Triple(0f, 0f, 0f)

        val skiDistance = runs.sumOf { it.distance.toDouble() }.toFloat()
        val skiVertical = runs.sumOf { it.verticalDrop.toDouble() }.toFloat()

        // Distance-weighted average speed
        val totalDist = runs.sumOf { it.distance.toDouble() }
        val avgSkiSpeed = if (totalDist > 0) {
            (runs.sumOf { it.avgSpeed * it.distance.toDouble() } / totalDist).toFloat()
        } else {
            0f
        }

        return Triple(skiDistance, skiVertical, avgSkiSpeed)
    }

    fun getTrackPoints(): List<TrackPoint> {
        return synchronized(trackPoints) {
            trackPoints.toList()
        }
    }

    fun getCurrentDistance(): Float {
        return _currentStats.value.distance
    }

    suspend fun startNewSession(): String {
        val sessionId = UUID.randomUUID().toString()
        startTime = System.currentTimeMillis()

        currentSession = RecordingSession(
            id = sessionId,
            startTime = startTime,
            isActive = true
        )

        trackPoints.clear()
        lastSavedIndex = 0
        _currentStats.value = StatsCalculator.TrackStats()
        _currentRuns.value = emptyList()  // Reset runs for new session

        sessionDao.insertSession(currentSession!!)

        return sessionId
    }

    suspend fun resumeSession(session: RecordingSession) {
        synchronized(trackPoints) {
            trackPoints.clear()
            lastSavedIndex = 0
        }
        currentSession = session
        startTime = session.startTime
        _currentStats.value = StatsCalculator.TrackStats(
            distance = session.distance,
            elevationGain = session.elevationGain,
            elevationLoss = session.elevationLoss,
            maxSpeed = session.maxSpeed,
            pointCount = session.pointCount
        )

        // Load existing points from temp file if available
        session.tempFilePath?.let { path ->
            loadPointsFromTempFile(path)
        }
        lastSavedIndex = trackPoints.size  // match loaded points
    }

    suspend fun saveTempGpx(): String? = withContext(Dispatchers.IO) {
        val session = currentSession ?: return@withContext null

        val pointsSnapshot: List<TrackPoint>
        val savedIndex: Int
        val isNewFile: Boolean

        synchronized(trackPoints) {
            if (lastSavedIndex >= trackPoints.size) return@withContext null

            val pointsToSave = trackPoints.subList(lastSavedIndex, trackPoints.size)
            if (pointsToSave.isEmpty()) return@withContext null

            pointsSnapshot = pointsToSave.toList()
            savedIndex = trackPoints.size
            val tempFile = getTempGpxFile()
            isNewFile = !tempFile.exists()
        }

        val tempFile = getTempGpxFile()

        if (isNewFile) {
            val gpxContent = GpxWriter.generateGpx(pointsSnapshot, "Temp Recording")
            tempFile.writeText(gpxContent)
        } else {
            appendPointsToGpx(tempFile, pointsSnapshot)
        }

        val updatedSession = synchronized(trackPoints) {
            lastSavedIndex = savedIndex

            session.copy(
                lastSavedPointIndex = lastSavedIndex,
                tempFilePath = tempFile.absolutePath,
                pointCount = trackPoints.size,
                distance = _currentStats.value.distance,
                elevationGain = _currentStats.value.elevationGain,
                elevationLoss = _currentStats.value.elevationLoss,
                maxSpeed = _currentStats.value.maxSpeed
            )
        }

        sessionDao.updateSession(updatedSession)
        currentSession = updatedSession

        return@withContext tempFile.absolutePath
    }

    suspend fun saveFinalGpx(): Pair<String, File>? = withContext(Dispatchers.IO) {
        val session = currentSession ?: return@withContext null

        val pointsSnapshot: List<TrackPoint>
        synchronized(trackPoints) {
            if (trackPoints.isEmpty()) return@withContext null
            pointsSnapshot = trackPoints.toList()
        }

        // Run final batch detection with full algorithm (includes segment combination)
        val finalRuns = RunDetector.detectRuns(pointsSnapshot)
        _currentRuns.value = finalRuns
        android.util.Log.i("GpxRepository", "Final batch detection: ${finalRuns.size} runs")

        val trackName = GpxWriter.generateTrackName()
        val gpxContent = GpxWriter.generateGpx(pointsSnapshot, trackName)

        val cacheDir = File(context.cacheDir, Constants.GPX_CACHE_DIR).apply {
            if (!exists()) mkdirs()
        }

        val finalFile = File(cacheDir, "$trackName${Constants.GPX_FILE_EXTENSION}")
        finalFile.writeText(gpxContent)

        // Persist track points to database before clearing in-memory data
        val trackPointEntities = pointsSnapshot.map { point ->
            com.skigpxrecorder.data.model.TrackPointEntity.fromTrackPoint(session.id, point)
        }
        trackPointDao.insertTrackPoints(trackPointEntities)

        // Persist final detected runs to database
        if (finalRuns.isNotEmpty()) {
            val skiRunEntities = finalRuns.map { run ->
                com.skigpxrecorder.data.model.SkiRunEntity.fromSkiRun(session.id, run)
            }
            skiRunDao.insertRuns(skiRunEntities)
        }

        sessionDao.markSessionInactive(session.id)

        getTempGpxFile().delete()

        return@withContext Pair(trackName, finalFile)
    }

    suspend fun saveToDownloads(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val downloadsDir = File(
                context.getExternalFilesDir(null),
                Constants.DOWNLOADS_SUBDIR
            ).apply {
                if (!exists()) mkdirs()
            }
            
            val destFile = File(downloadsDir, file.name)
            file.copyTo(destFile, overwrite = true)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun clearCurrentSession() {
        val sessionId = currentSession?.id
        currentSession = null
        trackPoints.clear()
        lastSavedIndex = 0
        lastRunDetectionTime = 0
        _currentStats.value = StatsCalculator.TrackStats()
        _currentRuns.value = emptyList()
        getTempGpxFile().delete()
        sessionId?.let { sessionDao.markSessionInactive(it) }
    }

    suspend fun getActiveSession(): RecordingSession? {
        return sessionDao.getActiveSession()
    }

    fun getActiveSessionFlow(): Flow<RecordingSession?> {
        return sessionDao.getActiveSessionFlow()
    }

    fun getCurrentSession(): RecordingSession? = currentSession

    suspend fun createImportedSession(
        gpxData: com.skigpxrecorder.data.model.GPXData,
        fileName: String
    ): String = withContext(Dispatchers.IO) {
        val sessionId = gpxData.metadata.sessionId
        val sessionName = gpxData.metadata.sessionName

        // Create session entity
        val session = RecordingSession(
            id = sessionId,
            startTime = gpxData.trackPoints.firstOrNull()?.timestamp ?: System.currentTimeMillis(),
            endTime = gpxData.trackPoints.lastOrNull()?.timestamp ?: System.currentTimeMillis(),
            distance = gpxData.stats.totalDistance,
            elevationGain = gpxData.stats.skiVertical,
            elevationLoss = gpxData.stats.totalDescent,
            maxSpeed = gpxData.stats.maxSpeed,
            pointCount = gpxData.trackPoints.size,
            isActive = false,
            tempFilePath = null,
            lastSavedPointIndex = gpxData.trackPoints.size,
            finalFilePath = null,
            sessionName = sessionName,
            runsCount = gpxData.runs.size,
            source = com.skigpxrecorder.data.model.DataSource.IMPORTED_GPX.name
        )

        // Insert session
        sessionDao.insertSession(session)

        // Insert track points
        val trackPointEntities = gpxData.trackPoints.map { point ->
            com.skigpxrecorder.data.model.TrackPointEntity.fromTrackPoint(sessionId, point)
        }
        trackPointDao.insertTrackPoints(trackPointEntities)

        // Insert runs
        val runEntities = gpxData.runs.map { run ->
            com.skigpxrecorder.data.model.SkiRunEntity.fromSkiRun(sessionId, run)
        }
        skiRunDao.insertRuns(runEntities)

        sessionId
    }

    private fun getTempGpxFile(): File {
        val cacheDir = File(context.cacheDir, Constants.GPX_CACHE_DIR).apply {
            if (!exists()) mkdirs()
        }
        return File(cacheDir, Constants.TEMP_GPX_FILE_NAME)
    }

    private fun loadPointsFromTempFile(path: String) {
        val file = File(path)
        if (!file.exists()) return

        try {
            val content = file.readText()
            val trkptPattern = """<trkpt lat="([^"]+)" lon="([^"]+)">""".toRegex()
            val elePattern = """<ele>([^<]+)</ele>""".toRegex()
            val timePattern = """<time>([^<]+)</time>""".toRegex()

            val lines = content.lines()
            var i = 0
            while (i < lines.size) {
                val trkptMatch = trkptPattern.find(lines[i])
                if (trkptMatch != null) {
                    val lat = trkptMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                    val lon = trkptMatch.groupValues[2].toDoubleOrNull() ?: 0.0

                    var ele = 0.0
                    var timestamp = System.currentTimeMillis()

                    for (j in (i + 1) until minOf(i + 5, lines.size)) {
                        elePattern.find(lines[j])?.let {
                            ele = it.groupValues[1].toDoubleOrNull() ?: 0.0
                        }
                        timePattern.find(lines[j])?.let {
                            val isoString = it.groupValues[1]
                            timestamp = try {
                                java.time.Instant.parse(isoString).toEpochMilli()
                            } catch (e: Exception) {
                                System.currentTimeMillis()
                            }
                        }
                    }

                    trackPoints.add(TrackPoint(lat, lon, ele, timestamp, 0f, 0f))
                }
                i++
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun appendPointsToGpx(file: File, points: List<TrackPoint>) {
        val content = file.readText()
        val insertPoint = content.lastIndexOf("</trkseg>")

        if (insertPoint == -1) return

        val newPointsXml = buildString {
            points.forEach { point ->
                appendLine("""      <trkpt lat="${point.latitude}" lon="${point.longitude}">""")
                appendLine("""        <ele>${point.elevation}</ele>""")
                appendLine("""        <time>${point.getIsoTimestamp()}</time>""")
                appendLine("""      </trkpt>""")
            }
        }

        val newContent = StringBuilder(content)
            .insert(insertPoint, newPointsXml)
            .toString()

        file.writeText(newContent)
    }

    /**
     * Get completed sessions flow for history screen
     */
    fun getCompletedSessions(): Flow<List<RecordingSession>> {
        return sessionDao.getCompletedSessions()
    }

    /**
     * Delete a session and all associated data
     */
    suspend fun deleteSessionFull(sessionId: String) = withContext(Dispatchers.IO) {
        // Delete from database (cascade will handle track points and runs)
        sessionDao.deleteSession(sessionId)

        // Delete associated GPX files if they exist
        val session = sessionDao.getSessionById(sessionId)
        session?.finalFilePath?.let { path ->
            File(path).delete()
        }
        session?.tempFilePath?.let { path ->
            File(path).delete()
        }
    }

    /**
     * Load complete session data as GPXData
     */
    suspend fun loadSessionData(sessionId: String): GPXData? = withContext(Dispatchers.IO) {
        val session = sessionDao.getSessionById(sessionId) ?: return@withContext null
        val trackPointEntities = trackPointDao.getTrackPointsForSession(sessionId)
        val skiRunEntities = skiRunDao.getRunsForSession(sessionId)

        val points = trackPointEntities.map { it.toTrackPoint() }
        val runs = skiRunEntities.map { it.toSkiRun() }

        SessionAnalyzer.analyzeSession(
            sessionId = sessionId,
            sessionName = session.sessionName ?: "Ski Session",
            points = points,
            source = DataSource.valueOf(session.source),
            isLive = false
        ).copy(runs = runs)
    }
}
