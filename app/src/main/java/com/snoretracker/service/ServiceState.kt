package com.snoretracker.service

import com.snoretracker.data.SnoreEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ServiceState {
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _currentDb = MutableStateFlow(0f)
    val currentDb: StateFlow<Float> = _currentDb.asStateFlow()

    private val _snoreEvents = MutableStateFlow<List<SnoreEvent>>(emptyList())
    val snoreEvents: StateFlow<List<SnoreEvent>> = _snoreEvents.asStateFlow()

    var sessionStartTime: Long = 0L

    var sensitivityThreshold: Float = 50f 
    var minSnoreDurationMs: Long = 800L
    var maxSnoreDurationMs: Long = 10000L

    fun setTracking(tracking: Boolean) {
        _isTracking.value = tracking
    }

    fun updateDb(db: Float) {
        _currentDb.value = db
    }

    fun addSnoreEvent(event: SnoreEvent) {
        _snoreEvents.value = _snoreEvents.value + event
    }

    fun clear() {
        _currentDb.value = 0f
        _snoreEvents.value = emptyList()
        sessionStartTime = 0L
    }
}
