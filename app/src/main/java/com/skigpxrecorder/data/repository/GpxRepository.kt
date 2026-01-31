package com.skigpxrecorder.data.repository

import android.content.Context
import com.skigpxrecorder.data.local.SessionDao
import com.skigpxrecorder.data.model.RecordingSession
import com.skigpxrecorder.data.model.TrackPoint
import com.skigpxrecorder.domain.GpxWriter
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
    private val sessionDao: SessionDao
) {
    private val trackPoints = mutableListOf<TrackPoint>()
    private var currentSession: RecordingSession? = null
    private var lastSavedIndex = 0

    private val _currentStats = MutableStateFlow(StatsCalculator.TrackStats())
    val currentStats: StateFlow<StatsCalculator.TrackStats> = _currentStats.asStateFlow()

    private var startTime: Long = 0

    fun addTrackPoint(point: TrackPoint) {
        synchronized(trackPoints) {
            val previousPoint = trackPoints.lastOrNull()
            trackPoints.add(point)
            
            val elapsedTime = (System.currentTimeMillis() - startTime) / 1000
            val newStats = StatsCalculator.calculateIncrementalStats(
                _currentStats.value,
                point,
                previousPoint,
                elapsedTime
            )
            _currentStats.value = newStats
        }
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
        
        sessionDao.insertSession(currentSession!!)
        
        return sessionId
    }

    suspend fun resumeSession(session: RecordingSession) {
        currentSession = session
        startTime = session.startTime
        lastSavedIndex = session.lastSavedPointIndex
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

        val trackName = GpxWriter.generateTrackName()
        val gpxContent = GpxWriter.generateGpx(pointsSnapshot, trackName)

        val cacheDir = File(context.cacheDir, Constants.GPX_CACHE_DIR).apply {
            if (!exists()) mkdirs()
        }

        val finalFile = File(cacheDir, "$trackName${Constants.GPX_FILE_EXTENSION}")
        finalFile.writeText(gpxContent)

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
        currentSession = null
        trackPoints.clear()
        lastSavedIndex = 0
        _currentStats.value = StatsCalculator.TrackStats()
        getTempGpxFile().delete()
    }

    suspend fun getActiveSession(): RecordingSession? {
        return sessionDao.getActiveSession()
    }

    fun getActiveSessionFlow(): Flow<RecordingSession?> {
        return sessionDao.getActiveSessionFlow()
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
}
