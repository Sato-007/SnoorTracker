package com.snoretracker.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("snore_prefs", Context.MODE_PRIVATE)

    fun getSensitivity(): Float = prefs.getFloat("sensitivity", 50f)
    fun setSensitivity(value: Float) = prefs.edit().putFloat("sensitivity", value).apply()

    fun getSilenceCooldown(): Long = prefs.getLong("silence_cooldown", 500L)
    fun setSilenceCooldown(value: Long) = prefs.edit().putLong("silence_cooldown", value).apply()

    fun getMinDuration(): Long = prefs.getLong("min_duration", 300L)
    fun setMinDuration(value: Long) = prefs.edit().putLong("min_duration", value).apply()

    fun getMaxDuration(): Long = prefs.getLong("max_duration", 3000L)
    fun setMaxDuration(value: Long) = prefs.edit().putLong("max_duration", value).apply()

    fun getEnableZcrFilter(): Boolean = prefs.getBoolean("enable_zcr", true)
    fun setEnableZcrFilter(value: Boolean) = prefs.edit().putBoolean("enable_zcr", value).apply()

    fun getAudioSource(): Int = prefs.getInt("audio_source", android.media.MediaRecorder.AudioSource.MIC)
    fun setAudioSource(value: Int) = prefs.edit().putInt("audio_source", value).apply()
}
