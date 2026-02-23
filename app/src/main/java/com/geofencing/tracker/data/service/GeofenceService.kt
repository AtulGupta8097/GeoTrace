package com.geofencing.tracker.data.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.geofencing.tracker.MainActivity
import com.geofencing.tracker.R
import com.geofencing.tracker.domain.model.GeofenceLocation
import com.geofencing.tracker.domain.model.GeofenceVisit
import com.geofencing.tracker.domain.repository.GeofenceRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@AndroidEntryPoint
class GeofenceService : Service() {

    @Inject lateinit var repository: GeofenceRepository
    @Inject lateinit var fusedLocationClient: FusedLocationProviderClient

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null
    private val currentlyInside = mutableSetOf<Long>()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            serviceScope.launch { checkProximity(loc.latitude, loc.longitude) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        acquireWakeLock()
        startForeground(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification("Monitoring geofences..."))
        startLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification("Monitoring geofences..."))
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (!hasLocationPermission()) return
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
            .setMinUpdateIntervalMillis(3_000L)
            .setMaxUpdateDelayMillis(10_000L)
            .build()
        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private suspend fun checkProximity(userLat: Double, userLng: Double) {
        val geofences = repository.getAllGeofencesSnapshot()
        updateNotificationText("📍 Tracking ${geofences.size} geofences")

        geofences.forEach { fence ->
            val distance = haversineMeters(userLat, userLng, fence.latitude, fence.longitude)
            val inside = distance <= fence.radius
            val wasInside = fence.id in currentlyInside
            when {
                inside && !wasInside -> { currentlyInside.add(fence.id); handleEnter(fence) }
                !inside && wasInside -> { currentlyInside.remove(fence.id); handleExit(fence) }
            }
        }

        guideToNextTarget(userLat, userLng, geofences)
    }

    private suspend fun handleEnter(fence: GeofenceLocation) {
        repository.addVisit(GeofenceVisit(geofenceId = fence.id, geofenceName = fence.name, entryTime = System.currentTimeMillis()))
        if (fence.isSelected) repository.markGeofenceVisited(fence.id, true)
        showEventNotification(fence.id.toInt() + 1000, "✅ Entered: ${fence.name}", "You arrived at this geofence!")
    }

    private suspend fun handleExit(fence: GeofenceLocation) {
        repository.getActiveVisit(fence.id)?.let {
            repository.updateVisitExitTime(it.id, System.currentTimeMillis())
        }
        showEventNotification(fence.id.toInt() + 2000, "👋 Exited: ${fence.name}", "You left this geofence")
    }

    private fun guideToNextTarget(userLat: Double, userLng: Double, allGeofences: List<GeofenceLocation>) {
        val selectedUnvisited = allGeofences
            .filter { it.isSelected && !it.isVisited }
            .sortedBy { haversineMeters(userLat, userLng, it.latitude, it.longitude) }

        if (selectedUnvisited.isEmpty()) {
            val selectedTotal = allGeofences.count { it.isSelected }
            if (selectedTotal > 0) updateNotificationText("🎉 All $selectedTotal route stops visited!")
            return
        }

        val next = selectedUnvisited.first()
        val distanceM = haversineMeters(userLat, userLng, next.latitude, next.longitude)
        val distanceText = if (distanceM >= 1000) "${"%.1f".format(distanceM / 1000)} km away" else "${distanceM.toInt()} m away"
        val remaining = selectedUnvisited.size
        updateNotificationText("🧭 Next: ${next.name} • $distanceText • $remaining stop${if (remaining == 1) "" else "s"} left")
    }

    private fun updateNotificationText(text: String) {
        getSystemService(NotificationManager::class.java).notify(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification(text))
    }

    private fun buildForegroundNotification(contentText: String) =
        NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
            .setContentTitle("Geofence Navigator")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    private fun showEventNotification(id: Int, title: String, message: String) {
        val notification = NotificationCompat.Builder(this, EVENT_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, id,
                    Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
        getSystemService(NotificationManager::class.java).notify(id, notification)
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(FOREGROUND_CHANNEL_ID, "Geofence Navigator", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "Persistent navigation guidance" }
        )
        nm.createNotificationChannel(
            NotificationChannel(EVENT_CHANNEL_ID, "Geofence Events", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Enter/exit alerts"; enableVibration(true) }
        )
    }

    private fun acquireWakeLock() {
        runCatching {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GeofenceApp::NavigatorWakeLock")
                .apply { acquire(24 * 60 * 60 * 1000L) }
        }
    }

    private fun releaseWakeLock() {
        runCatching { wakeLock?.takeIf { it.isHeld }?.release() }
        wakeLock = null
    }

    private fun hasLocationPermission() =
        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6_371_000.0
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val dPhi = Math.toRadians(lat2 - lat1)
        val dLambda = Math.toRadians(lng2 - lng1)
        val a = sin(dPhi / 2).pow(2) + cos(phi1) * cos(phi2) * sin(dLambda / 2).pow(2)
        return r * 2 * asin(sqrt(a))
    }

    companion object {
        const val FOREGROUND_CHANNEL_ID = "geofence_nav_channel"
        const val EVENT_CHANNEL_ID = "geofence_event_channel"
        private const val FOREGROUND_NOTIFICATION_ID = 1
    }
}
