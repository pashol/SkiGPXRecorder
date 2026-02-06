package com.skigpxrecorder.ui.import

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.skigpxrecorder.domain.FileImporter
import com.skigpxrecorder.util.Constants
import kotlinx.coroutines.launch

/**
 * Composable handler for file import using ActivityResultContracts.OpenDocument
 */
@Composable
fun FileImportHandler(
    fileImporter: FileImporter,
    onImportSuccess: (String) -> Unit,
    onImportError: (String) -> Unit,
    trigger: Boolean = false,
    onTriggerConsumed: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            selectedUri = uri
        }
    )

    // Launch file picker when trigger is true
    LaunchedEffect(trigger) {
        if (trigger) {
            filePickerLauncher.launch(
                arrayOf(
                    "application/gpx+xml",
                    Constants.FIT_MIME_TYPE,
                    "application/octet-stream",
                    "*/*"
                )
            )
            onTriggerConsumed()
        }
    }

    // Process selected file
    LaunchedEffect(selectedUri) {
        selectedUri?.let { uri ->
            android.util.Log.d("FileImportHandler", "Processing URI: $uri")
            scope.launch {
                try {
                    when (val result = fileImporter.importFile(uri)) {
                        is FileImporter.ImportResult.Success -> {
                            android.util.Log.d("FileImportHandler", "Import success, sessionId: ${result.gpxData.metadata.sessionId}")
                            onImportSuccess(result.gpxData.metadata.sessionId)
                        }
                        is FileImporter.ImportResult.Error -> {
                            android.util.Log.e("FileImportHandler", "Import error: ${result.message}")
                            onImportError(result.message)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FileImportHandler", "Import exception", e)
                    onImportError("Import failed: ${e.message}")
                }
                selectedUri = null
            }
        }
    }
}
