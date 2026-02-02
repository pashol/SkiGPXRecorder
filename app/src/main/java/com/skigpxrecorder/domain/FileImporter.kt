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
        android.util.Log.d("FileImporter", "Starting import for URI: $uri")
        return try {
            val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                ?: return ImportResult.Error("Could not open file").also {
                    android.util.Log.e("FileImporter", "Could not open input stream for URI: $uri")
                }

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
            android.util.Log.d("FileImporter", "Saving session to database: ${gpxData.metadata.sessionId}")
            gpxRepository.createImportedSession(gpxData, fileName)
            android.util.Log.d("FileImporter", "Session saved successfully")
            ImportResult.Success(gpxData)
        } catch (e: Exception) {
            ImportResult.Error("GPX parsing failed: ${e.message}")
        }
    }

    private fun getFileName(uri: Uri): String {
        // Try to get display name from content resolver first
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val displayNameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex >= 0) {
                    val displayName = cursor.getString(displayNameIndex)
                    if (!displayName.isNullOrEmpty()) {
                        return displayName
                    }
                }
            }
        }
        
        // Fallback to URI path
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
