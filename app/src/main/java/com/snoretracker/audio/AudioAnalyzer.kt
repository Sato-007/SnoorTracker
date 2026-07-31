package com.snoretracker.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.snoretracker.data.SnoreEvent
import com.snoretracker.service.ServiceState
import com.snoretracker.util.AudioUtils
import com.snoretracker.util.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.abs

class AudioAnalyzer(private val context: Context) {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2

    @SuppressLint("MissingPermission")
    suspend fun start() = withContext(Dispatchers.IO) {
        if (!PermissionHelper.hasMicrophonePermission(context)) {
            Log.e("AudioAnalyzer", "Microphone permission not granted")
            return@withContext
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioAnalyzer", "AudioRecord initialization failed")
                return@withContext
            }

            audioRecord?.startRecording()
            isRecording = true
            ServiceState.setTracking(true)
            ServiceState.sessionStartTime = System.currentTimeMillis()

            val buffer = ShortArray(bufferSize)
            var consecutiveHighDb = 0
            var consecutiveLowDb = 0
            var isCurrentlySnoring = false
            var currentSnoreStartTime = 0L
            var currentSnorePeakDb = 0f
            
            while (isActive && isRecording) {
                val readResult = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                if (readResult > 0) {
                    var maxAmplitude = 0
                    for (i in 0 until readResult) {
                        val amplitude = abs(buffer[i].toInt())
                        if (amplitude > maxAmplitude) {
                            maxAmplitude = amplitude
                        }
                    }

                    val db = AudioUtils.calculateDecibels(maxAmplitude)
                    ServiceState.updateDb(db)

                    if (db >= ServiceState.sensitivityThreshold) {
                        consecutiveHighDb++
                        consecutiveLowDb = 0
                        
                        if (!isCurrentlySnoring && consecutiveHighDb >= 2) {
                            isCurrentlySnoring = true
                            currentSnoreStartTime = System.currentTimeMillis() - 200 // Account for the 2 chunks
                            currentSnorePeakDb = db
                        } else if (isCurrentlySnoring) {
                            if (db > currentSnorePeakDb) {
                                currentSnorePeakDb = db
                            }
                        }
                    } else {
                        consecutiveHighDb = 0
                        if (isCurrentlySnoring) {
                            consecutiveLowDb++
                            if (consecutiveLowDb >= 5) { // 500ms cooldown
                                val duration = System.currentTimeMillis() - currentSnoreStartTime - 500
                                if (duration in ServiceState.minSnoreDurationMs..ServiceState.maxSnoreDurationMs) {
                                    val event = SnoreEvent(
                                        timestamp = currentSnoreStartTime,
                                        peakDb = currentSnorePeakDb,
                                        durationMs = duration
                                    )
                                    ServiceState.addSnoreEvent(event)
                                }
                                isCurrentlySnoring = false
                                consecutiveLowDb = 0
                            }
                        } else {
                            consecutiveLowDb = 0
                        }
                    }
                }
                delay(100) 
            }
            
            // Flush any ongoing snore when stopping
            if (isCurrentlySnoring) {
                val duration = System.currentTimeMillis() - currentSnoreStartTime
                if (duration in ServiceState.minSnoreDurationMs..ServiceState.maxSnoreDurationMs) {
                    val event = SnoreEvent(
                        timestamp = currentSnoreStartTime,
                        peakDb = currentSnorePeakDb,
                        durationMs = duration
                    )
                    ServiceState.addSnoreEvent(event)
                }
            }
        } catch (e: Exception) {
            Log.e("AudioAnalyzer", "Error recording audio", e)
        } finally {
            stop()
        }
    }

    fun stop() {
        isRecording = false
        ServiceState.setTracking(false)
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            Log.e("AudioAnalyzer", "Error stopping AudioRecord", e)
        }
    }
}
