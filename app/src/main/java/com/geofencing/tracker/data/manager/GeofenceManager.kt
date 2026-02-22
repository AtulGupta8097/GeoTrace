package com.geofencing.tracker.data.manager

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.geofencing.tracker.domain.model.GeofenceLocation
import com.geofencing.tracker.domain.repository.GeofenceRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Replaces Google's GeofencingClient entirely.
 *
 * Subscribes to [FusedLocationProviderClient] location updates and manually
 * computes haversine distance against every stored geofence.
 * This avoids any dependency on the Google Geofencing API.
 *
 * Call [startMonitoring] once (e.g. from a foreground service) and
 * [stopMonitoring] when done.
 */
@Singleton
class GeofenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val repository: GeofenceRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Called with the geofence and whether the user just entered (true) or exited (false). */
    var onTransition: ((geofence: GeofenceLocation, entered: Boolean) -> Unit)? = null

    // Track which geofences the user is currently inside to detect enter/exit edges
    private val currentlyInside = mutableSetOf<Long>()

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        10_000L  // 10 s interval — adjust for battery vs. responsiveness
    ).setMinUpdateIntervalMillis(5_000L).build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            scope.launch {
                checkProximity(loc.latitude, loc.longitude)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startMonitoring() {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun stopMonitoring() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        scope.cancel()
    }

    private suspend fun checkProximity(userLat: Double, userLng: Double) {
        val geofences = repository.getAllGeofencesSnapshot()
        geofences.forEach { fence ->
            val distance = haversineMeters(userLat, userLng, fence.latitude, fence.longitude)
            val inside = distance <= fence.radius
            val wasInside = fence.id in currentlyInside

            when {
                inside && !wasInside -> {
                    currentlyInside.add(fence.id)
                    onTransition?.invoke(fence, true)
                }
                !inside && wasInside -> {
                    currentlyInside.remove(fence.id)
                    onTransition?.invoke(fence, false)
                }
            }
        }
    }

    // ── Haversine distance ────────────────────────────────────────────────────

    private fun haversineMeters(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double
    ): Double {
        val r = 6_371_000.0  // Earth radius in metres
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val dPhi = Math.toRadians(lat2 - lat1)
        val dLambda = Math.toRadians(lng2 - lng1)

        val a = sin(dPhi / 2).pow(2) +
                cos(phi1) * cos(phi2) * sin(dLambda / 2).pow(2)
        return r * 2 * asin(sqrt(a))
    }
}