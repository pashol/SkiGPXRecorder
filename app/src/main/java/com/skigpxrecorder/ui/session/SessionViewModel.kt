package com.skigpxrecorder.ui.session

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skigpxrecorder.data.local.SessionDao
import com.skigpxrecorder.data.local.SkiRunDao
import com.skigpxrecorder.data.local.TrackPointDao
import com.skigpxrecorder.data.model.DataSource
import com.skigpxrecorder.data.model.GPXData
import com.skigpxrecorder.data.model.SessionMetadata
import com.skigpxrecorder.data.repository.GpxRepository
import com.skigpxrecorder.domain.SessionAnalyzer
import com.skigpxrecorder.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Shared ViewModel for session views (Track, Map, Analysis, Profile)
 * Scoped to the session navigation graph
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionDao: SessionDao,
    private val trackPointDao: TrackPointDao,
    private val skiRunDao: SkiRunDao,
    private val gpxRepository: GpxRepository
) : ViewModel() {

    sealed class ExportState {
        object Idle : ExportState()
        object Loading : ExportState()
        data class Success(val uri: Uri) : ExportState()
        data class Error(val message: String) : ExportState()
    }

    private val _gpxData = MutableStateFlow<GPXData?>(null)
    val gpxData: StateFlow<GPXData?> = _gpxData.asStateFlow()

    private val _selectedRunIndex = MutableStateFlow<Int?>(null)
    val selectedRunIndex: StateFlow<Int?> = _selectedRunIndex.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    /**
     * Load session data from database or live repository
     */
    fun loadSession(sessionId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                // Check if this is the active session (live recording)
                val activeSession = gpxRepository.getCurrentSession()
                val isLive = activeSession?.id == sessionId

                if (isLive) {
                    // Load from live repository
                    loadLiveSession(sessionId)
                } else {
                    // Load from database
                    loadHistoricalSession(sessionId)
                }
            } catch (e: Exception) {
                android.util.Log.e("SessionViewModel", "Error loading session", e)
                _gpxData.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load live recording session from repository
     */
    private suspend fun loadLiveSession(sessionId: String) {
        val session = sessionDao.getSessionById(sessionId) ?: return
        val currentSession = gpxRepository.getCurrentSession() ?: return

        // For live sessions, we get data from the repository's in-memory state
        // This will be enhanced in Phase 11 to subscribe to live updates
        val points = gpxRepository.getTrackPoints()
        val runs = emptyList<com.skigpxrecorder.data.model.SkiRun>() // Live run detection in Phase 11

        val gpxData = SessionAnalyzer.analyzeSession(
            sessionId = sessionId,
            sessionName = session.sessionName ?: "Live Recording",
            points = points,
            source = DataSource.RECORDED,
            isLive = true
        )

        _gpxData.value = gpxData
    }

    /**
     * Load completed session from database
     */
    private suspend fun loadHistoricalSession(sessionId: String) {
        val session = sessionDao.getSessionById(sessionId) ?: return
        val trackPointEntities = trackPointDao.getTrackPointsForSession(sessionId)
        val skiRunEntities = skiRunDao.getRunsForSession(sessionId)

        val points = trackPointEntities.map { it.toTrackPoint() }
        val runs = skiRunEntities.map { it.toSkiRun() }

        val gpxData = SessionAnalyzer.analyzeSession(
            sessionId = sessionId,
            sessionName = session.sessionName ?: "Ski Session",
            points = points,
            source = DataSource.valueOf(session.source),
            isLive = false,
            existingRuns = runs
        )

        _gpxData.value = gpxData
    }

    /**
     * Select a specific run for viewing details
     */
    fun selectRun(runIndex: Int?) {
        _selectedRunIndex.value = runIndex
    }

    /**
     * Navigate to next run
     */
    fun nextRun() {
        val currentData = _gpxData.value ?: return
        val currentIndex = _selectedRunIndex.value ?: -1
        val nextIndex = (currentIndex + 1).coerceAtMost(currentData.runs.size - 1)
        if (nextIndex >= 0) {
            _selectedRunIndex.value = nextIndex
        }
    }

    /**
     * Navigate to previous run
     */
    fun previousRun() {
        val currentIndex = _selectedRunIndex.value ?: 0
        val prevIndex = (currentIndex - 1).coerceAtLeast(0)
        _selectedRunIndex.value = prevIndex
    }

    /**
     * Export current session as GPX file for sharing
     */
    fun exportSession(context: Context) {
        val sessionId = _gpxData.value?.metadata?.sessionId ?: return

        viewModelScope.launch {
            _exportState.value = ExportState.Loading
            try {
                val file = gpxRepository.exportSessionAsGpx(sessionId)
                if (file != null) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}${Constants.FILE_PROVIDER_AUTHORITY_SUFFIX}",
                        file
                    )
                    _exportState.value = ExportState.Success(uri)
                    android.util.Log.d("SessionViewModel", "Export successful: $uri")
                } else {
                    _exportState.value = ExportState.Error("Failed to generate GPX file")
                    android.util.Log.e("SessionViewModel", "Export failed: file is null")
                }
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Unknown error")
                android.util.Log.e("SessionViewModel", "Export exception", e)
            }
        }
    }

    /**
     * Reset export state to Idle
     */
    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }
}
