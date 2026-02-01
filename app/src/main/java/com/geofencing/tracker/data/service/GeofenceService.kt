package com.geofencing.tracker.data.service

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.geofencing.tracker.GeofenceApplication
import com.geofencing.tracker.R
import com.geofencing.tracker.domain.repository.GeofenceRepository
import com.geofencing.tracker.domain.usecase.RecordGeofenceEntryUseCase
import com.geofencing.tracker.domain.usecase.RecordGeofenceExitUseCase
import com.google.android.gms.location.Geofence
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val geofenceId = intent?.getLongExtra("geofence_id", -1L) ?: -1L
        val transitionType = intent?.getIntExtra("transition_type", -1) ?: -1

        if (geofenceId != -1L) {
            serviceScope.launch {
                handleGeofenceTransition(geofenceId, transitionType)
                stopSelf(startId)
            }
        }

        return START_NOT_STICKY
    }

    private suspend fun handleGeofenceTransition(geofenceId: Long, transitionType: Int) {
        val geofence = repository.getGeofence(geofenceId) ?: return

        when (transitionType) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                recordEntryUseCase(geofenceId, geofence.name)
                showNotification("Entered ${geofence.name}", "You have entered the geofenced area")
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                recordExitUseCase(geofenceId)
                showNotification("Exited ${geofence.name}", "You have exited the geofenced area")
            }
        }
    }

    private fun showNotification(title: String, message: String) {
        val notification = NotificationCompat.Builder(this, GeofenceApplication.GEOFENCE_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}