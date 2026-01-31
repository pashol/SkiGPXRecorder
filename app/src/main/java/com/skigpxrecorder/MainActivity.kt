package com.skigpxrecorder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.skigpxrecorder.ui.RecordingScreen
import com.skigpxrecorder.ui.RecordingViewModel
import com.skigpxrecorder.ui.theme.SkiGPXRecorderTheme
import com.skigpxrecorder.util.PermissionHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: RecordingViewModel by viewModels()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        
        if (locationGranted) {
            viewModel.onPermissionGranted()
        } else {
            viewModel.onPermissionDenied()
        }

        // Handle notification permission if requested
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val notificationGranted = permissions[android.Manifest.permission.POST_NOTIFICATIONS] == true
            viewModel.onNotificationPermissionResult(notificationGranted)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        setContent {
            SkiGPXRecorderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RecordingScreen(
                        viewModel = viewModel,
                        onRequestPermission = { checkAndRequestPermissions() }
                    )
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        when {
            PermissionHelper.hasLocationPermission(this) -> {
                viewModel.onPermissionGranted()
            }
            else -> {
                val permissions = PermissionHelper.getAllRequiredPermissions()
                locationPermissionLauncher.launch(permissions)
            }
        }
    }

    private fun observeViewModel() {
        // Removed - share state is handled directly in Compose UI
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkForActiveSession()
    }
}
