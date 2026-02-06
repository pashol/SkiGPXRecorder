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
    private val userPreferences: UserPreferences
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
     * Grouped sessions based on current grouping mode
     */
    val groupedSessions: StateFlow<List<SessionGroup>> = combine(
        sessions,
        groupingMode
    ) { sessionList, mode ->
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
        // Group by approximate location using elevation (placeholder)
        // Full implementation would use geocoding for each session
        return sessions
            .sortedByDescending { it.startTime }
            .groupBy { session ->
                // For now, group by elevation range as a simple placeholder
                val elevRange = (session.elevationGain / 100).toInt() * 100
                "Location (~${elevRange}m elevation)"
            }
            .map { (location, sessionList) ->
                SessionGroup(
                    header = "📍 $location",
                    sessions = sessionList
                )
            }
    }
}
