package com.skigpxrecorder.ui.session.rundetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skigpxrecorder.data.model.SkiRun
import com.skigpxrecorder.data.model.TrackPoint
import com.skigpxrecorder.data.repository.GpxRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for run detail screen
 */
@HiltViewModel
class RunDetailViewModel @Inject constructor(
    private val gpxRepository: GpxRepository
) : ViewModel() {

    private val _run = MutableStateFlow<SkiRun?>(null)
    val run: StateFlow<SkiRun?> = _run.asStateFlow()

    private val _runPoints = MutableStateFlow<List<TrackPoint>>(emptyList())
    val runPoints: StateFlow<List<TrackPoint>> = _runPoints.asStateFlow()

    private val _allRuns = MutableStateFlow<List<SkiRun>>(emptyList())
    val allRuns: StateFlow<List<SkiRun>> = _allRuns.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Load run details
     */
    fun loadRun(sessionId: String, runNumber: Int) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val gpxData = gpxRepository.loadSessionData(sessionId)
                if (gpxData != null) {
                    val targetRun = gpxData.runs.find { it.runNumber == runNumber }
                    _run.value = targetRun
                    _allRuns.value = gpxData.runs

                    // Extract points for this run
                    targetRun?.let { run ->
                        val points = gpxData.trackPoints.subList(
                            run.startIndex.coerceAtLeast(0),
                            (run.endIndex + 1).coerceAtMost(gpxData.trackPoints.size)
                        )
                        _runPoints.value = points
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("RunDetailViewModel", "Error loading run", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
