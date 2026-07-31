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
                        if (consecutiveHighDb >= 2) {
                            val event = SnoreEvent(
                                timestamp = System.currentTimeMillis(),
                                peakDb = db,
                                durationMs = 100L 
                            )
                            ServiceState.addSnoreEvent(event)
                            consecutiveHighDb = 0 
                        }
                    } else {
                        consecutiveHighDb = 0
                    }
                }
                delay(100) 
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
