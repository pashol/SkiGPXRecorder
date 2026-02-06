package com.skigpxrecorder.domain

import android.content.Context
import android.net.Uri
import com.skigpxrecorder.data.model.DataSource
import com.skigpxrecorder.data.model.GPXData
import com.skigpxrecorder.data.repository.GpxRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
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
        android.util.Log.d("FileImporter", "URI scheme: ${uri.scheme}, authority: ${uri.authority}, path: ${uri.path}")

        return try {
            // Try to open the input stream with detailed error handling
            val inputStream: InputStream = try {
                context.contentResolver.openInputStream(uri)
            } catch (e: SecurityException) {
                android.util.Log.e("FileImporter", "SecurityException opening stream", e)
                return ImportResult.Error("Permission denied. Please try selecting the file again.")
            } catch (e: java.io.FileNotFoundException) {
                android.util.Log.e("FileImporter", "FileNotFoundException", e)
                return ImportResult.Error("File not found. It may have been moved or deleted.")
            } catch (e: Exception) {
                android.util.Log.e("FileImporter", "Exception opening stream: ${e.javaClass.simpleName}", e)
                return ImportResult.Error("Could not read file: ${e.message}")
            } ?: run {
                android.util.Log.e("FileImporter", "openInputStream returned null for URI: $uri")
                android.util.Log.e("FileImporter", "This usually means the file picker didn't grant read permission")
                return ImportResult.Error("Could not access file. Please try again or select a different file.")
            }

            android.util.Log.d("FileImporter", "Successfully opened input stream")

            val fileName = getFileName(uri)
            android.util.Log.d("FileImporter", "File name: $fileName")
            val fileType = getFileType(fileName)
            android.util.Log.d("FileImporter", "File type: $fileType")

            when (fileType) {
                FileType.GPX -> importGpxFile(inputStream, fileName)
                FileType.FIT -> importFitFile(inputStream, fileName)
                FileType.UNKNOWN -> {
                    inputStream.close()
                    ImportResult.Error("Unsupported file type. Please select a .gpx or .fit file.")
                }
            }
        } catch (e: SecurityException) {
            android.util.Log.e("FileImporter", "SecurityException during import", e)
            ImportResult.Error("Permission error: ${e.message}")
        } catch (e: Exception) {
            android.util.Log.e("FileImporter", "Exception during import: ${e.javaClass.simpleName}", e)
            ImportResult.Error("Import failed: ${e.message ?: "Unknown error"}")
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

    private suspend fun importFitFile(
        inputStream: InputStream,
        fileName: String
    ): ImportResult {
        return try {
            val parseResult = FitParser.parse(inputStream)

            if (parseResult.trackPoints.isEmpty()) {
                return ImportResult.Error("No track points found in FIT file")
            }

            // Use metadata name if available, otherwise use filename
            val sessionName = parseResult.metadata.name
                ?: fileName.removeSuffix(".fit")

            // Analyze the session
            val sessionId = "imported_${System.currentTimeMillis()}"
            val gpxData = sessionAnalyzer.analyzeSession(
                sessionId = sessionId,
                sessionName = sessionName,
                points = parseResult.trackPoints,
                source = DataSource.IMPORTED_FIT,
                isLive = false
            )
            android.util.Log.d("FileImporter", "Saving FIT session to database: ${gpxData.metadata.sessionId}")
            gpxRepository.createImportedSession(gpxData, fileName)
            android.util.Log.d("FileImporter", "FIT session saved successfully")
            ImportResult.Success(gpxData)
        } catch (e: Exception) {
            ImportResult.Error("FIT parsing failed: ${e.message}")
        }
    }

    private fun getFileName(uri: Uri): String {
        // Try to get display name from content resolver first
        try {
            context.contentResolver.query(
                uri,
                arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayNameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex >= 0) {
                        val displayName = cursor.getString(displayNameIndex)
                        if (!displayName.isNullOrEmpty()) {
                            android.util.Log.d("FileImporter", "Got file name from cursor: $displayName")
                            return displayName
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("FileImporter", "Could not query display name: ${e.message}")
        }

        // Fallback to URI path
        val path = uri.path ?: return "unknown.gpx"
        val fileName = path.substringAfterLast('/')
        android.util.Log.d("FileImporter", "Got file name from path: $fileName")
        return fileName
    }

    /**
     * Copy content from URI to temporary file as fallback for problematic content providers
     */
    private fun copyToTempFile(uri: Uri, inputStream: InputStream): File {
        val tempDir = File(context.cacheDir, "imports")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }

        val fileName = getFileName(uri)
        val tempFile = File(tempDir, "temp_$fileName")

        android.util.Log.d("FileImporter", "Copying to temp file: ${tempFile.absolutePath}")

        inputStream.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        android.util.Log.d("FileImporter", "Successfully copied to temp file, size: ${tempFile.length()} bytes")
        return tempFile
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
