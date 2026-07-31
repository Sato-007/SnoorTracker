package com.snoretracker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.snoretracker.data.SnoreDatabase
import com.snoretracker.data.SnoreEvent
import com.snoretracker.data.SnoreRepository
import com.snoretracker.data.SnoreSession
import com.snoretracker.service.ServiceState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TrackingStatus { IDLE, ACTIVE, SUMMARY }

data class TrackerUiState(
    val trackingStatus: TrackingStatus = TrackingStatus.IDLE,
    val currentDb: Float = 0f,
    val amplitudeHistory: List<Float> = emptyList(),
    val sensitivityThreshold: Float = 50f,
    val minSnoreDurationMs: Long = 800L,
    val maxSnoreDurationMs: Long = 10000L,
    val sessionStartTime: Long = 0L,
    val elapsedTimeMs: Long = 0L,
    val snoreEventCount: Int = 0,
    val peakDb: Float = 0f,
    val totalSnoreDurationMs: Long = 0L,
    val snoreEvents: List<SnoreEvent> = emptyList(),
    val hasMicPermission: Boolean = false,
    val hasNotificationPermission: Boolean = false,
)

data class HistoryUiState(
    val sessions: List<SnoreSession> = emptyList(),
    val selectedSession: SnoreSession? = null,
    val isLoading: Boolean = false,
)

class SnoreViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SnoreRepository = SnoreRepository(SnoreDatabase.getDatabase(application).snoreDao())

    private val _trackerUiState = MutableStateFlow(TrackerUiState())
    val trackerUiState = _trackerUiState.asStateFlow()

    private val _historyUiState = MutableStateFlow(HistoryUiState(isLoading = true))
    val historyUiState = _historyUiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allSessions.collect { sessions ->
                _historyUiState.update { it.copy(sessions = sessions, isLoading = false) }
            }
        }

        viewModelScope.launch {
            combine(
                ServiceState.isTracking,
                ServiceState.currentDb,
                ServiceState.snoreEvents
            ) { isTracking, db, events ->
                Triple(isTracking, db, events)
            }.collect { (isTracking, db, events) ->
                _trackerUiState.update { state ->
                    val status = if (isTracking) TrackingStatus.ACTIVE else state.trackingStatus
                    
                    val newHistory = if (isTracking) {
                        val history = state.amplitudeHistory.toMutableList()
                        history.add(db)
                        if (history.size > 40) history.removeAt(0)
                        history
                    } else emptyList()

                    state.copy(
                        trackingStatus = status,
                        currentDb = db,
                        amplitudeHistory = newHistory,
                        snoreEvents = events,
                        snoreEventCount = events.size,
                        peakDb = events.maxOfOrNull { it.peakDb } ?: 0f,
                        totalSnoreDurationMs = events.sumOf { it.durationMs },
                        sessionStartTime = ServiceState.sessionStartTime
                    )
                }
            }
        }
        
        viewModelScope.launch {
            while(true) {
                if (_trackerUiState.value.trackingStatus == TrackingStatus.ACTIVE) {
                    _trackerUiState.update {
                        it.copy(elapsedTimeMs = System.currentTimeMillis() - it.sessionStartTime)
                    }
                }
                delay(1000)
            }
        }
    }

    fun setSensitivity(db: Float) {
        ServiceState.sensitivityThreshold = db
        _trackerUiState.update { it.copy(sensitivityThreshold = db) }
    }

    fun setDurationRange(minMs: Long, maxMs: Long) {
        ServiceState.minSnoreDurationMs = minMs
        ServiceState.maxSnoreDurationMs = maxMs
        _trackerUiState.update { it.copy(minSnoreDurationMs = minMs, maxSnoreDurationMs = maxMs) }
    }

    fun setPermissions(mic: Boolean, notif: Boolean) {
        _trackerUiState.update { it.copy(hasMicPermission = mic, hasNotificationPermission = notif) }
    }

    fun markSessionStopped() {
        _trackerUiState.update { it.copy(trackingStatus = TrackingStatus.SUMMARY) }
    }

    fun resetTracker() {
        _trackerUiState.update { 
            TrackerUiState(
                trackingStatus = TrackingStatus.IDLE,
                hasMicPermission = it.hasMicPermission,
                hasNotificationPermission = it.hasNotificationPermission,
                sensitivityThreshold = it.sensitivityThreshold,
                minSnoreDurationMs = it.minSnoreDurationMs,
                maxSnoreDurationMs = it.maxSnoreDurationMs
            )
        }
    }

    fun selectHistorySession(session: SnoreSession?) {
        _historyUiState.update { it.copy(selectedSession = session) }
    }

    fun deleteSession(session: SnoreSession) {
        viewModelScope.launch {
            repository.deleteSession(session)
            if (_historyUiState.value.selectedSession?.id == session.id) {
                selectHistorySession(null)
            }
        }
    }
}
