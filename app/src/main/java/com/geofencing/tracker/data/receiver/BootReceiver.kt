package com.geofencing.tracker.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.geofencing.tracker.data.manager.GeofenceManager
import com.geofencing.tracker.domain.repository.GeofenceRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: GeofenceRepository

    @Inject
    lateinit var geofenceManager: GeofenceManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d(TAG, "Boot completed, restoring geofences")

            val pendingResult = goAsync()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

            scope.launch {
                try {
                    val geofences = repository.getAllGeofences().firstOrNull() ?: emptyList()
                    Log.d(TAG, "Found ${geofences.size} geofences to restore")

                    geofences.forEach { geofence ->
                        geofenceManager.addGeofence(geofence)
                        Log.d(TAG, "Restored geofence: ${geofence.name}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error restoring geofences", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}