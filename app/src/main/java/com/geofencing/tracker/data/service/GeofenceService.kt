package com.geofencing.tracker.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.geofencing.tracker.MainActivity
import com.geofencing.tracker.R
import com.geofencing.tracker.data.receiver.GeofenceBroadcastReceiver.Companion.EXTRA_GEOFENCE_ID
import com.geofencing.tracker.data.receiver.GeofenceBroadcastReceiver.Companion.EXTRA_TRANSITION_TYPE
import com.geofencing.tracker.domain.repository.GeofenceRepository
import com.geofencing.tracker.domain.usecase.RecordGeofenceEntryUseCase
import com.geofencing.tracker.domain.usecase.RecordGeofenceExitUseCase
import com.google.android.gms.location.Geofence
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class GeofenceService : Service() {

    @Inject
    lateinit var repository: GeofenceRepository

    @Inject
    lateinit var recordEntryUseCase: RecordGeofenceEntryUseCase

    @Inject
    lateinit var recordExitUseCase: RecordGeofenceExitUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "Service created")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")

        acquireWakeLock()

        startForeground(NOTIFICATION_ID, createForegroundNotification())

        val geofenceId = intent?.getLongExtra(EXTRA_GEOFENCE_ID, -1L) ?: -1L
        val transitionType = intent?.getIntExtra(EXTRA_TRANSITION_TYPE, -1) ?: -1

        if (geofenceId == -1L || transitionType == -1) {
            Log.e(TAG, "Invalid intent extras")
            stopSelfSafely(startId)
            return START_STICKY
        }

        serviceScope.launch {
            try {
                handleGeofenceTransition(geofenceId, transitionType)
                delay(1000)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling geofence", e)
            } finally {
                withContext(Dispatchers.Main) {
                    stopSelfSafely(startId)
                }
            }
        }

        return START_STICKY
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "GeofenceApp::GeofenceWakeLock"
            ).apply {
                acquire(60000)
            }
            Log.d(TAG, "WakeLock acquired")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake lock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d(TAG, "WakeLock released")
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release wake lock", e)
        }
    }

    private suspend fun handleGeofenceTransition(geofenceId: Long, transitionType: Int) {
        Log.d(TAG, "Handling transition for geofence: $geofenceId, type: $transitionType")

        val geofence = repository.getGeofence(geofenceId)
        if (geofence == null) {
            Log.e(TAG, "Geofence not found: $geofenceId")
            return
        }

        when (transitionType) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                Log.d(TAG, "ENTER: ${geofence.name}")
                recordEntryUseCase(geofenceId, geofence.name)
                showEventNotification(
                    "Entered ${geofence.name}",
                    "You have entered the geofenced area"
                )
            }

            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                Log.d(TAG, "EXIT: ${geofence.name}")
                recordExitUseCase(geofenceId)
                showEventNotification(
                    "Exited ${geofence.name}",
                    "You have exited the geofenced area"
                )
            }

            else -> {
                Log.w(TAG, "Unknown transition type: $transitionType")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Geofence Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for geofence events"
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }

    private fun createForegroundNotification() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Geofence Service")
            .setContentText("Monitoring geofence events")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

    private fun showEventNotification(title: String, message: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)

        Log.d(TAG, "Notification shown: $title")
    }

    private fun stopSelfSafely(startId: Int) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf(startId)
            releaseWakeLock()
            Log.d(TAG, "Service stopped safely")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping service", e)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "GeofenceService"
        private const val CHANNEL_ID = "geofence_channel"
        private const val NOTIFICATION_ID = 12345
    }
}