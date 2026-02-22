package com.geofencing.tracker.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.geofencing.tracker.data.service.GeofenceService
import com.google.android.gms.location.GeofencingEvent



/**
 * Previously handled Google GeofencingClient broadcasts.
 * Google Geofencing has been removed — proximity detection is now done entirely
 * inside GeofenceManager using FusedLocationProvider + haversine distance checks.
 *
 * This file is kept only to preserve the EXTRA_* constants referenced by
 * GeofenceService. The receiver itself is no longer registered in the manifest.
 */
object GeofenceBroadcastReceiver {
    const val EXTRA_GEOFENCE_ID = "geofence_id"
    const val EXTRA_TRANSITION_TYPE = "transition_type"

    // Transition type constants — replaces com.google.android.gms.location.Geofence constants
    const val TRANSITION_ENTER = 1
    const val TRANSITION_EXIT = 2
}