package com.skigpxrecorder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.skigpxrecorder.ui.RecordingScreen
import com.skigpxrecorder.domain.FileImporter
import com.skigpxrecorder.ui.RecordingViewModel
import com.skigpxrecorder.ui.import.FileImportHandler
import com.skigpxrecorder.ui.navigation.AppDrawer
import com.skigpxrecorder.ui.navigation.AppNavigation
import com.skigpxrecorder.ui.navigation.BottomNavBar
import com.skigpxrecorder.ui.navigation.Screen
import com.skigpxrecorder.ui.theme.SkiGPXRecorderTheme
import com.skigpxrecorder.util.PermissionHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: RecordingViewModel by viewModels()

    @Inject
    lateinit var fileImporter: FileImporter

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
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()
                val snackbarHostState = remember { SnackbarHostState() }
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                var triggerFileImport by remember { mutableStateOf(false) }

                // Get current route for bottom nav
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // File import handler
                FileImportHandler(
                    fileImporter = fileImporter,
                    onImportSuccess = { sessionId ->
                        scope.launch {
                            snackbarHostState.showSnackbar("File imported successfully")
                            navController.navigate(Screen.Session.createRoute(sessionId))
                        }
                    },
                    onImportError = { error ->
                        scope.launch {
                            snackbarHostState.showSnackbar("Import failed: $error")
                        }
                    },
                    trigger = triggerFileImport,
                    onTriggerConsumed = { triggerFileImport = false }
                )

                // Determine if bottom nav should be shown
                val showBottomNav = currentRoute in listOf(
                    "start",
                    "session_history",
                    "highscore"
                )

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = false,  // Disable swipe gesture to prevent conflict with map
                    drawerContent = {
                        AppDrawer(
                            navController = navController,
                            drawerState = drawerState,
                            scope = scope,
                            onOpenFile = { triggerFileImport = true }
                        )
                    }
                ) {
                    Scaffold(
                        bottomBar = {
                            if (showBottomNav) {
                                BottomNavBar(
                                    currentRoute = currentRoute,
                                    navController = navController
                                )
                            }
                        },
                        snackbarHost = { SnackbarHost(snackbarHostState) }
                    ) { paddingValues ->
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            AppNavigation(
                                navController = navController,
                                recordingViewModel = viewModel,
                                onRequestPermission = { checkAndRequestPermissions() },
                                onOpenFile = { triggerFileImport = true },
                                drawerState = drawerState
                            )
                        }
                    }
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
