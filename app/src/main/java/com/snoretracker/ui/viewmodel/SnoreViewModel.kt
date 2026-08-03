package com.snoretracker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.snoretracker.data.SettingsManager
import com.snoretracker.data.SnoreDatabase
import com.snoretracker.data.SnoreEvent
import com.snoretracker.data.SnoreRepository
import com.snoretracker.data.SnoreSession
import com.snoretracker.service.ServiceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TrackingStatus { IDLE, ACTIVE, SUMMARY }

data class TrackerUiState(
    val trackingStatus: TrackingStatus = TrackingStatus.IDLE,
    val currentDb: Float = 0f,
    val amplitudeHistory: List<Float> = emptyList(),
    val sensitivityThreshold: Float = 50f,
    val silenceCooldownMs: Long = 500L,
    val minSnoreDurationMs: Long = 300L,
    val maxSnoreDurationMs: Long = 3000L,
    val enableZcrFilter: Boolean = true,
    val audioSource: Int = android.media.MediaRecorder.AudioSource.MIC,
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
    private val settingsManager = SettingsManager(application)

    private val _trackerUiState = MutableStateFlow(
        TrackerUiState(
            sensitivityThreshold = settingsManager.getSensitivity(),
            silenceCooldownMs = settingsManager.getSilenceCooldown(),
            minSnoreDurationMs = settingsManager.getMinDuration(),
            maxSnoreDurationMs = settingsManager.getMaxDuration(),
            enableZcrFilter = settingsManager.getEnableZcrFilter(),
            audioSource = settingsManager.getAudioSource()
        )
    )
    val trackerUiState = _trackerUiState.asStateFlow()

    private val _historyUiState = MutableStateFlow(HistoryUiState(isLoading = true))
    val historyUiState = _historyUiState.asStateFlow()

    init {
        ServiceState.sensitivityThreshold = settingsManager.getSensitivity()
        ServiceState.silenceCooldownMs = settingsManager.getSilenceCooldown()
        ServiceState.minSnoreDurationMs = settingsManager.getMinDuration()
        ServiceState.maxSnoreDurationMs = settingsManager.getMaxDuration()
        ServiceState.enableZcrFilter = settingsManager.getEnableZcrFilter()
        ServiceState.audioSource = settingsManager.getAudioSource()

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
            trackerUiState
                .map { it.trackingStatus == TrackingStatus.ACTIVE }
                .distinctUntilChanged()
                .collectLatest { isActive ->
                    while (isActive) {
                        _trackerUiState.update {
                            it.copy(elapsedTimeMs = System.currentTimeMillis() - it.sessionStartTime)
                        }
                        delay(1000)
                    }
                }
        }
    }

    fun setSensitivity(db: Float) {
        settingsManager.setSensitivity(db)
        ServiceState.sensitivityThreshold = db
        _trackerUiState.update { it.copy(sensitivityThreshold = db) }
    }

    fun setSilenceCooldown(ms: Long) {
        settingsManager.setSilenceCooldown(ms)
        ServiceState.silenceCooldownMs = ms
        _trackerUiState.update { it.copy(silenceCooldownMs = ms) }
    }

    fun setDurationRange(minMs: Long, maxMs: Long) {
        settingsManager.setMinDuration(minMs)
        settingsManager.setMaxDuration(maxMs)
        ServiceState.minSnoreDurationMs = minMs
        ServiceState.maxSnoreDurationMs = maxMs
        _trackerUiState.update { it.copy(minSnoreDurationMs = minMs, maxSnoreDurationMs = maxMs) }
    }

    fun setEnableZcrFilter(enabled: Boolean) {
        settingsManager.setEnableZcrFilter(enabled)
        ServiceState.enableZcrFilter = enabled
        _trackerUiState.update { it.copy(enableZcrFilter = enabled) }
    }

    fun setAudioSource(source: Int) {
        settingsManager.setAudioSource(source)
        ServiceState.audioSource = source
        _trackerUiState.update { it.copy(audioSource = source) }
    }

    fun setPermissions(mic: Boolean, notif: Boolean) {
        _trackerUiState.update { it.copy(hasMicPermission = mic, hasNotificationPermission = notif) }
    }

    fun markSessionStopped() {
        ServiceState.setTracking(false)
        val state = _trackerUiState.value
        if (state.sessionStartTime > 0) {
            val events = state.snoreEvents
            val session = SnoreSession(
                startTime = state.sessionStartTime,
                endTime = System.currentTimeMillis(),
                totalSnoreEvents = events.size,
                totalSnoreDurationMs = events.sumOf { it.durationMs },
                peakDb = events.maxOfOrNull { it.peakDb } ?: 0f,
                events = events
            )
            
            viewModelScope.launch(Dispatchers.IO) {
                repository.saveSession(session)
            }
        }
        _trackerUiState.update { it.copy(trackingStatus = TrackingStatus.SUMMARY) }
    }

    fun resetTracker() {
        _trackerUiState.update { 
            TrackerUiState(
                trackingStatus = TrackingStatus.IDLE,
                hasMicPermission = it.hasMicPermission,
                hasNotificationPermission = it.hasNotificationPermission,
                sensitivityThreshold = it.sensitivityThreshold,
                silenceCooldownMs = it.silenceCooldownMs,
                minSnoreDurationMs = it.minSnoreDurationMs,
                maxSnoreDurationMs = it.maxSnoreDurationMs,
                enableZcrFilter = it.enableZcrFilter,
                audioSource = it.audioSource
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

    fun deleteAllSessions() {
        viewModelScope.launch {
            repository.deleteAllSessions()
            selectHistorySession(null)
        }
    }
}
