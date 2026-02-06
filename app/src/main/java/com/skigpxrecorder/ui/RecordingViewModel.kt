package com.skigpxrecorder.ui

import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skigpxrecorder.data.model.RecordingSession
import com.skigpxrecorder.data.model.TrackPoint
import com.skigpxrecorder.data.repository.GpxRepository
import com.skigpxrecorder.domain.StatsCalculator
import com.skigpxrecorder.service.LocationServiceManager
import com.skigpxrecorder.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import android.content.Context

@HiltViewModel
class RecordingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationServiceManager: LocationServiceManager,
    private val gpxRepository: GpxRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordingUiState())
    val uiState: StateFlow<RecordingUiState> = _uiState.asStateFlow()

    private var startTime: Long = 0
    private var timerJob: kotlinx.coroutines.Job? = null

    private var locationFlowJob: kotlinx.coroutines.Job? = null
    private var batteryFlowJob: kotlinx.coroutines.Job? = null

    init {
        // Observe service connection and subscribe to flows
        viewModelScope.launch {
            locationServiceManager.serviceBoundFlow.collect { isBound ->
                if (isBound) {
                    val service = locationServiceManager.getService()
                    if (service != null) {
                        locationFlowJob?.cancel()
                        locationFlowJob = viewModelScope.launch {
                            service.locationFlow.collect { _ ->
                                // Update track points in UI state
                                _uiState.update {
                                    it.copy(trackPoints = gpxRepository.getTrackPoints())
                                }
                            }
                        }

                        batteryFlowJob?.cancel()
                        batteryFlowJob = viewModelScope.launch {
                            service.batteryWarningFlow.collect { _ ->
                                _uiState.update { it.copy(batteryWarning = true) }
                            }
                        }

                        // Race fix: if service was already recording when we bind, sync UI
                        if (service.isCurrentlyRecording() && !_uiState.value.isRecording) {
                            syncWithRunningService()
                        }
                    }
                } else {
                    locationFlowJob?.cancel()
                    batteryFlowJob?.cancel()
                }
            }
        }

        // Observe repository stats
        gpxRepository.currentStats
            .onEach { stats ->
                _uiState.update { currentState ->
                    currentState.copy(
                        currentSpeed = stats.currentSpeed,
                        distance = stats.distance,
                        currentElevation = stats.currentElevation,
                        gpsAccuracy = stats.currentAccuracy,
                        maxSpeed = stats.maxSpeed,
                        elevationGain = stats.elevationGain,
                        elevationLoss = stats.elevationLoss,
                        avgSpeed = stats.avgSpeed,
                        pointCount = stats.pointCount,
                        skiDistance = stats.skiDistance,
                        skiVertical = stats.skiVertical,
                        avgSkiSpeed = stats.avgSkiSpeed
                    )
                }
            }
            .launchIn(viewModelScope)

        // Observe repository runs
        gpxRepository.currentRuns
            .onEach { runs ->
                _uiState.update { it.copy(runCount = runs.size) }
            }
            .launchIn(viewModelScope)

        // Bind to service
        locationServiceManager.bindService()
    }

    fun onPermissionGranted() {
        _uiState.update { it.copy(hasLocationPermission = true, showPermissionDenied = false) }
    }

    fun onPermissionDenied() {
        _uiState.update { it.copy(hasLocationPermission = false, showPermissionDenied = true) }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasNotificationPermission = granted) }
    }

    fun checkForActiveSession() {
        if (_uiState.value.isRecording) return
        if (locationServiceManager.isRecording()) {
            syncWithRunningService()
            return
        }
        viewModelScope.launch {
            val activeSession = gpxRepository.getActiveSession()
            if (activeSession != null) {
                _uiState.update {
                    it.copy(
                        showResumeDialog = true,
                        interruptedSession = activeSession
                    )
                }
            }
        }
    }

    private fun syncWithRunningService() {
        val session = gpxRepository.getCurrentSession()
        startTime = session?.startTime ?: System.currentTimeMillis()
        _uiState.update {
            it.copy(
                isRecording = true,
                showResumeDialog = false,
                interruptedSession = null,
                startTime = startTime
            )
        }
        startTimer()
    }

    fun onResumeSessionConfirmed() {
        val session = _uiState.value.interruptedSession ?: return

        viewModelScope.launch {
            gpxRepository.resumeSession(session)
            startTime = session.startTime
            _uiState.update {
                it.copy(
                    isRecording = true,
                    showResumeDialog = false,
                    startTime = session.startTime
                )
            }
            startTimer()
            locationServiceManager.startRecording()
        }
    }

    fun onDiscardSession() {
        val sessionId = _uiState.value.interruptedSession?.id
        viewModelScope.launch {
            gpxRepository.clearCurrentSession(sessionId)
            _uiState.update {
                it.copy(
                    showResumeDialog = false,
                    interruptedSession = null
                )
            }
        }
    }

    fun startRecording() {
        if (!_uiState.value.hasLocationPermission) {
            _uiState.update { it.copy(showPermissionDenied = true) }
            return
        }

        viewModelScope.launch {
            gpxRepository.startNewSession()
            startTime = System.currentTimeMillis()
            
            _uiState.update { 
                it.copy(
                    isRecording = true,
                    startTime = startTime,
                    recordingFilePath = null
                )
            }
            
            startTimer()
            locationServiceManager.startRecording()
        }
    }

    fun stopRecording() {
        timerJob?.cancel()

        viewModelScope.launch {
            val result = gpxRepository.saveFinalGpx()
            gpxRepository.clearCurrentSession()

            locationServiceManager.stopRecording()

            _uiState.update {
                it.copy(
                    isRecording = false,
                    recordingFilePath = result?.second?.absolutePath,
                    showShareOptions = result != null,
                    savedTrackName = result?.first
                )
            }
        }
    }

    fun shareGpxFile() {
        val filePath = _uiState.value.recordingFilePath ?: return
        val file = File(filePath)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}${Constants.FILE_PROVIDER_AUTHORITY_SUFFIX}",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = Constants.GPX_MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Ski Track GPX File")
            putExtra(Intent.EXTRA_TEXT, "Here's my ski track recorded with Ski GPX Recorder")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Share GPX File")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun saveToDownloads() {
        val filePath = _uiState.value.recordingFilePath ?: return
        val file = File(filePath)
        
        viewModelScope.launch {
            val success = gpxRepository.saveToDownloads(file)
            _uiState.update { 
                it.copy(
                    showShareOptions = false,
                    showSaveSuccess = success
                )
            }
            
            // Clear success message after delay
            delay(3000)
            _uiState.update { it.copy(showSaveSuccess = false) }
        }
    }

    fun dismissBatteryWarning() {
        _uiState.update { it.copy(batteryWarning = false) }
    }

    fun dismissPermissionDenied() {
        _uiState.update { it.copy(showPermissionDenied = false) }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val elapsed = (System.currentTimeMillis() - startTime) / 1000
                _uiState.update { it.copy(elapsedTime = elapsed) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        locationServiceManager.unbindService()
    }
}

data class RecordingUiState(
    val isRecording: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val hasNotificationPermission: Boolean = true,
    val showPermissionDenied: Boolean = false,
    val showResumeDialog: Boolean = false,
    val interruptedSession: RecordingSession? = null,
    val batteryWarning: Boolean = false,
    val showShareOptions: Boolean = false,
    val showSaveSuccess: Boolean = false,
    val recordingFilePath: String? = null,
    val savedTrackName: String? = null,
    val startTime: Long = 0,

    // Live stats
    val currentSpeed: Float = 0f,
    val distance: Float = 0f,
    val elapsedTime: Long = 0L,
    val currentElevation: Float = 0f,
    val gpsAccuracy: Float = 0f,
    val maxSpeed: Float = 0f,
    val elevationGain: Float = 0f,
    val elevationLoss: Float = 0f,
    val avgSpeed: Float = 0f,
    val pointCount: Int = 0,
    val runCount: Int = 0,

    // Ski-specific stats
    val skiDistance: Float = 0f,
    val skiVertical: Float = 0f,
    val avgSkiSpeed: Float = 0f,

    // Track points for live map
    val trackPoints: List<TrackPoint> = emptyList()
)
