package com.skigpxrecorder.ui.import

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            android.util.Log.d("FileImportHandler", "File picker returned URI: $uri")
            if (uri != null) {
                android.util.Log.d("FileImportHandler", "URI details - scheme: ${uri.scheme}, authority: ${uri.authority}, path: ${uri.path}")

                // Grant read permission for this URI
                // This ensures we can access the file even if it's from a cloud provider
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    android.util.Log.d("FileImportHandler", "Took persistable URI permission")
                } catch (e: SecurityException) {
                    // Persistable permission not available (e.g., from some cloud providers)
                    // This is OK - we still have temporary permission
                    android.util.Log.d("FileImportHandler", "Could not take persistable permission (this is normal for some providers): ${e.message}")
                }
            } else {
                android.util.Log.d("FileImportHandler", "File picker returned null (user cancelled)")
            }
            selectedUri = uri
        }
    )

    // Launch file picker when trigger is true
    LaunchedEffect(trigger) {
        if (trigger) {
            android.util.Log.d("FileImportHandler", "Launching file picker")
            // Use "*/*" to allow all files, then we'll validate based on file extension
            // This is more reliable than specific MIME types which vary by provider
            filePickerLauncher.launch(arrayOf("*/*"))
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
