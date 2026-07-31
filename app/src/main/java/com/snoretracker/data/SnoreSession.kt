package com.snoretracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "snore_session")
data class SnoreSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val totalSnoreEvents: Int,
    val totalSnoreDurationMs: Long,
    val peakDb: Float,
    val events: List<SnoreEvent>
)
