package com.geofencing.tracker.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.geofencing.tracker.data.service.GeofenceService

import com.google.android.gms.location.GeofencingEvent
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences

        triggeringGeofences?.forEach { geofence ->
            val geofenceId = geofence.requestId.toLongOrNull() ?: return@forEach
            
            val serviceIntent = Intent(context, GeofenceService::class.java).apply {
                putExtra("geofence_id", geofenceId)
                putExtra("transition_type", geofenceTransition)
            }
            context.startService(serviceIntent)
        }
    }
}