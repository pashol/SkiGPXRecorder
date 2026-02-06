package com.skigpxrecorder.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skigpxrecorder.data.local.UserPreferences
import com.skigpxrecorder.data.model.RecordingSession
import com.skigpxrecorder.data.repository.GpxRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Data class for grouped sessions
 */
data class SessionGroup(
    val header: String,
    val sessions: List<RecordingSession>
)

/**
 * ViewModel for session history screen
 */
@HiltViewModel
class SessionHistoryViewModel @Inject constructor(
    private val gpxRepository: GpxRepository,
    private val userPreferences: UserPreferences,
    private val geocodingService: com.skigpxrecorder.domain.GeocodingService,
    private val trackPointDao: com.skigpxrecorder.data.local.TrackPointDao
) : ViewModel() {

    /**
     * Flow of completed sessions
     */
    val sessions: StateFlow<List<RecordingSession>> = gpxRepository
        .getCompletedSessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Current grouping mode
     */
    val groupingMode: StateFlow<UserPreferences.HistoryGrouping> = userPreferences.historyGroupingFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences.HistoryGrouping.DATE
        )

    /**
     * Cache for session locations to avoid repeated geocoding
     */
    private val locationCache = mutableMapOf<String, String>()
    private val _locationCacheUpdates = MutableStateFlow(0)

    /**
     * Grouped sessions based on current grouping mode
     */
    val groupedSessions: StateFlow<List<SessionGroup>> = combine(
        sessions,
        groupingMode,
        _locationCacheUpdates
    ) { sessionList, mode, _ ->
        when (mode) {
            UserPreferences.HistoryGrouping.DATE -> groupByDate(sessionList)
            UserPreferences.HistoryGrouping.LOCATION -> groupByLocation(sessionList)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Set grouping mode
     */
    fun setGroupingMode(mode: UserPreferences.HistoryGrouping) {
        viewModelScope.launch {
            userPreferences.setHistoryGrouping(mode)
        }
    }

    /**
     * Delete a session
     */
    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            gpxRepository.deleteSessionFull(sessionId)
        }
    }

    /**
     * Group sessions by date (month/year)
     */
    private fun groupByDate(sessions: List<RecordingSession>): List<SessionGroup> {
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        return sessions
            .sortedByDescending { it.startTime }
            .groupBy { session ->
                dateFormat.format(Date(session.startTime))
            }
            .map { (date, sessionList) ->
                SessionGroup(
                    header = "📅 $date",
                    sessions = sessionList
                )
            }
    }

    /**
     * Group sessions by location (using reverse geocoding)
     */
    private fun groupByLocation(sessions: List<RecordingSession>): List<SessionGroup> {
        // Trigger geocoding for sessions without cached locations
        sessions.forEach { session ->
            if (!locationCache.containsKey(session.id)) {
                viewModelScope.launch {
                    getLocationForSession(session.id)
                }
            }
        }

        // Group by cached location names
        return sessions
            .sortedByDescending { it.startTime }
            .groupBy { session ->
                locationCache[session.id] ?: "Loading location..."
            }
            .filter { (location, _) ->
                // Filter out "Loading..." groups if they're empty
                location != "Loading location..." || _locationCacheUpdates.value > 0
            }
            .map { (location, sessionList) ->
                SessionGroup(
                    header = "📍 $location",
                    sessions = sessionList
                )
            }
    }

    /**
     * Get location name for a session using reverse geocoding
     */
    private suspend fun getLocationForSession(sessionId: String) {
        try {
            // Get first track point for this session
            val trackPoints = trackPointDao.getTrackPointsForSession(sessionId)
            if (trackPoints.isNotEmpty()) {
                val firstPoint = trackPoints.first()
                val locationName = geocodingService.getLocationName(
                    latitude = firstPoint.latitude,
                    longitude = firstPoint.longitude
                )
                locationCache[sessionId] = locationName
                _locationCacheUpdates.value += 1
            } else {
                locationCache[sessionId] = "Unknown Location"
                _locationCacheUpdates.value += 1
            }
        } catch (e: Exception) {
            android.util.Log.e("SessionHistoryViewModel", "Failed to geocode session $sessionId", e)
            locationCache[sessionId] = "Unknown Location"
            _locationCacheUpdates.value += 1
        }
    }
}
