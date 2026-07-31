package com.snoretracker.data

import com.google.gson.annotations.SerializedName

data class SnoreEvent(
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("peakDb") val peakDb: Float,
    @SerializedName("durationMs") val durationMs: Long
)
