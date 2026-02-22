package com.geofencing.tracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.geofencing.tracker.data.service.GeofenceService
import dagger.hilt.android.HiltAndroidApp
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer

@HiltAndroidApp
class GeofenceApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        MapLibre.getInstance(this, null, WellKnownTileServer.MapLibre)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NotificationManager::class.java)

        // Persistent foreground (low importance — doesn't make sound)
        nm.createNotificationChannel(
            NotificationChannel(
                GeofenceService.FOREGROUND_CHANNEL_ID,
                "Geofence Navigator",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Persistent navigation guidance" }
        )

        // Enter/exit events (high importance — makes sound + vibration)
        nm.createNotificationChannel(
            NotificationChannel(
                GeofenceService.EVENT_CHANNEL_ID,
                "Geofence Events",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Enter/exit alerts"
                enableVibration(true)
            }
        )
    }
}
