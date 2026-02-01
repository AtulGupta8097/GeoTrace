package com.geofencing.tracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GeofenceApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            GEOFENCE_CHANNEL_ID,
            "Geofence Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for geofence events"
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val GEOFENCE_CHANNEL_ID = "geofence_channel"
    }
}