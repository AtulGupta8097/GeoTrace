package com.geofencing.tracker.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.geofencing.tracker.data.service.GeofenceService
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent == null) {
            return
        }

        if (geofencingEvent.hasError()) {
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences

        if (triggeringGeofences.isNullOrEmpty()) {
            return
        }

        triggeringGeofences.forEach { geofence ->
            val geofenceId = geofence.requestId.toLongOrNull()
            if (geofenceId == null) {
                return@forEach
            }

            val serviceIntent = Intent(context, GeofenceService::class.java).apply {
                putExtra(EXTRA_GEOFENCE_ID, geofenceId)
                putExtra(EXTRA_TRANSITION_TYPE, geofenceTransition)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }

    companion object {
        private const val TAG = "GeofenceBroadcast"
        const val EXTRA_GEOFENCE_ID = "geofence_id"
        const val EXTRA_TRANSITION_TYPE = "transition_type"
    }
}