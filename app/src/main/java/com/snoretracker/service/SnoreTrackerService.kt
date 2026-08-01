package com.snoretracker.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.snoretracker.audio.AudioAnalyzer
import com.snoretracker.data.SnoreDatabase
import com.snoretracker.data.SnoreRepository
import com.snoretracker.data.SnoreSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import android.content.pm.ServiceInfo
import android.os.Build

class SnoreTrackerService : Service() {
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var audioAnalyzer: AudioAnalyzer? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationHelper.buildTrackingNotification(this)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(1, notification)
        }

        audioAnalyzer = AudioAnalyzer(this)
        serviceScope.launch {
            ServiceState.clear()
            audioAnalyzer?.start()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        audioAnalyzer?.stop()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
