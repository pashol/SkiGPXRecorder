package com.skigpxrecorder.domain

import android.content.Context
import android.net.Uri
import com.skigpxrecorder.data.model.DataSource
import com.skigpxrecorder.data.model.GPXData
import com.skigpxrecorder.data.repository.GpxRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates file import from GPX files
 */
@Singleton
class FileImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionAnalyzer: SessionAnalyzer,
    private val gpxRepository: GpxRepository
) {

    sealed class ImportResult {
        data class Success(val gpxData: GPXData) : ImportResult()
        data class Error(val message: String) : ImportResult()
    }

    suspend fun importFile(uri: Uri): ImportResult {
        return try {
            val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                ?: return ImportResult.Error("Could not open file")

            val fileName = getFileName(uri)
            val fileType = getFileType(fileName)

            when (fileType) {
                FileType.GPX -> importGpxFile(inputStream, fileName)
                FileType.FIT -> ImportResult.Error("FIT file import not yet supported")
                FileType.UNKNOWN -> ImportResult.Error("Unsupported file type")
            }
        } catch (e: Exception) {
            ImportResult.Error("Import failed: ${e.message}")
        }
    }

    private suspend fun importGpxFile(
        inputStream: InputStream,
        fileName: String
    ): ImportResult {
        return try {
            val parseResult = GpxParser.parse(inputStream)

            if (parseResult.trackPoints.isEmpty()) {
                return ImportResult.Error("No track points found in file")
            }

            // Use metadata name if available, otherwise use filename
            val sessionName = parseResult.metadata.name
                ?: fileName.removeSuffix(".gpx")

            // Analyze the session
            val sessionId = "imported_${System.currentTimeMillis()}"
            val gpxData = sessionAnalyzer.analyzeSession(
                sessionId = sessionId,
                sessionName = sessionName,
                points = parseResult.trackPoints,
                source = DataSource.IMPORTED_GPX,
                isLive = false
            )
            gpxRepository.createImportedSession(gpxData, fileName)
            ImportResult.Success(gpxData)
        } catch (e: Exception) {
            ImportResult.Error("GPX parsing failed: ${e.message}")
        }
    }

    private fun getFileName(uri: Uri): String {
        val path = uri.path ?: return "unknown.gpx"
        return path.substringAfterLast('/')
    }

    private fun getFileType(fileName: String): FileType {
        return when {
            fileName.endsWith(".gpx", ignoreCase = true) -> FileType.GPX
            fileName.endsWith(".fit", ignoreCase = true) -> FileType.FIT
            else -> FileType.UNKNOWN
        }
    }

    private enum class FileType {
        GPX, FIT, UNKNOWN
    }
}
