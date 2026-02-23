package com.geofencing.tracker.presentation.route

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geofencing.tracker.domain.model.GeofenceLocation
import com.geofencing.tracker.domain.repository.GeofenceRepository
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.maplibre.android.geometry.LatLng
import org.maplibre.geojson.Point
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class RouteState(
    val orderedStops: List<GeofenceLocation> = emptyList(),
    val currentStopIndex: Int = 0,
    val userLocation: LatLng? = null,
    val routePoints: List<Point> = emptyList(),
    val legDistanceMeters: Double = 0.0,
    val legDurationSeconds: Double = 0.0,
    val distanceToNextMeters: Double? = null,
    val isLoading: Boolean = true,
    val isFetchingRoute: Boolean = false,
    val allVisited: Boolean = false,
    val error: String? = null
) {
    val nextStop: GeofenceLocation? get() = orderedStops.getOrNull(currentStopIndex)
    val visitedCount: Int get() = orderedStops.count { it.isVisited }
}

@HiltViewModel
class RouteViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: GeofenceRepository,
    private val fusedLocationClient: FusedLocationProviderClient
) : ViewModel() {

    private val _state = MutableStateFlow(RouteState())
    val state: StateFlow<RouteState> = _state.asStateFlow()

    private var locationPollJob: Job? = null

    init { loadRoute() }

    fun loadRoute() {
        viewModelScope.launch {
            _state.value = RouteState(isLoading = true)
            val userLoc = getUserLocation()
            val selected = repository.getSelectedGeofences()

            if (selected.isEmpty()) {
                _state.value = RouteState(isLoading = false, userLocation = userLoc)
                return@launch
            }

            val ordered = if (userLoc != null) sortNearestFirst(userLoc.latitude, userLoc.longitude, selected) else selected
            val startIndex = ordered.indexOfFirst { !it.isVisited }.coerceAtLeast(0)

            _state.value = RouteState(
                orderedStops = ordered,
                currentStopIndex = startIndex,
                userLocation = userLoc,
                isLoading = false,
                isFetchingRoute = userLoc != null,
                allVisited = ordered.all { it.isVisited }
            )

            val nextStop = ordered.getOrNull(startIndex)
            if (!ordered.all { it.isVisited } && userLoc != null && nextStop != null) {
                fetchLeg(userLoc, nextStop)
            }

            startLocationPolling()
        }
    }

    fun resetRoute() {
        locationPollJob?.cancel()
        viewModelScope.launch {
            repository.clearAllSelections()
            repository.clearAllVisited()
            _state.value = RouteState(isLoading = false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationPollJob?.cancel()
    }

    private fun startLocationPolling() {
        locationPollJob?.cancel()
        locationPollJob = viewModelScope.launch {
            while (isActive) {
                val loc = getUserLocation()
                if (loc != null) {
                    val s = _state.value
                    val nextStop = s.nextStop
                    if (nextStop != null && !s.allVisited) {
                        val dist = haversineMeters(loc.latitude, loc.longitude, nextStop.latitude, nextStop.longitude)
                        _state.value = s.copy(userLocation = loc, distanceToNextMeters = dist)
                        if (dist <= nextStop.radius) onArrived(nextStop, loc)
                    }
                }
                delay(5_000)
            }
        }
    }

    private suspend fun onArrived(stop: GeofenceLocation, userLoc: LatLng) {
        repository.markGeofenceVisited(stop.id, true)
        val s = _state.value
        val nextIndex = s.currentStopIndex + 1
        val refreshed = s.orderedStops.map { if (it.id == stop.id) it.copy(isVisited = true) else it }
        val allDone = nextIndex >= refreshed.size || refreshed.all { it.isVisited }

        if (allDone) {
            _state.value = s.copy(orderedStops = refreshed, currentStopIndex = nextIndex.coerceAtMost(refreshed.size), routePoints = emptyList(), allVisited = true, distanceToNextMeters = null)
        } else {
            val nextStop = refreshed[nextIndex]
            _state.value = s.copy(
                orderedStops = refreshed,
                currentStopIndex = nextIndex,
                userLocation = userLoc,
                routePoints = emptyList(),
                isFetchingRoute = true,
                distanceToNextMeters = haversineMeters(userLoc.latitude, userLoc.longitude, nextStop.latitude, nextStop.longitude)
            )
            fetchLeg(userLoc, nextStop)
        }
    }

    private suspend fun fetchLeg(from: LatLng, to: GeofenceLocation) {
        _state.value = _state.value.copy(isFetchingRoute = true, error = null)
        try {
            val coords = "${from.longitude},${from.latitude};${to.longitude},${to.latitude}"
            val url = "https://router.project-osrm.org/route/v1/driving/$coords?overview=full&geometries=geojson&steps=false"

            val json = withContext(Dispatchers.IO) {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 10_000
                conn.readTimeout = 15_000
                try {
                    if (conn.responseCode == 200) conn.inputStream.bufferedReader().readText() else null
                } finally {
                    conn.disconnect()
                }
            }

            if (json == null) {
                _state.value = _state.value.copy(isFetchingRoute = false, error = "Could not fetch route — check internet connection")
                return
            }

            val root = JSONObject(json)
            if (root.optString("code") != "Ok") {
                _state.value = _state.value.copy(isFetchingRoute = false, error = "Routing error: ${root.optString("code")}")
                return
            }

            val route = root.getJSONArray("routes").getJSONObject(0)
            val coordsArr = route.getJSONObject("geometry").getJSONArray("coordinates")
            val points = (0 until coordsArr.length()).map { i ->
                val pair = coordsArr.getJSONArray(i)
                Point.fromLngLat(pair.getDouble(0), pair.getDouble(1))
            }

            _state.value = _state.value.copy(
                routePoints = points,
                legDistanceMeters = route.getDouble("distance"),
                legDurationSeconds = route.getDouble("duration"),
                isFetchingRoute = false,
                error = null
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(isFetchingRoute = false, error = "Route fetch failed: ${e.message}")
        }
    }

    private fun sortNearestFirst(startLat: Double, startLng: Double, stops: List<GeofenceLocation>): List<GeofenceLocation> {
        val remaining = stops.toMutableList()
        val result = mutableListOf<GeofenceLocation>()
        var curLat = startLat
        var curLng = startLng
        while (remaining.isNotEmpty()) {
            val next = remaining.minByOrNull { haversineMeters(curLat, curLng, it.latitude, it.longitude) }!!
            result.add(next)
            remaining.remove(next)
            curLat = next.latitude
            curLng = next.longitude
        }
        return result
    }

    private suspend fun getUserLocation(): LatLng? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return null
        return try {
            val last = fusedLocationClient.lastLocation.await()
            if (last != null) {
                LatLng(last.latitude, last.longitude)
            } else {
                val fresh = fusedLocationClient.getCurrentLocation(
                    CurrentLocationRequest.Builder()
                        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                        .build(),
                    null
                ).await()
                fresh?.let { LatLng(it.latitude, it.longitude) }
            }
        } catch (e: Exception) { null }
    }

    private fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6_371_000.0
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val dPhi = Math.toRadians(lat2 - lat1)
        val dLambda = Math.toRadians(lng2 - lng1)
        val a = sin(dPhi / 2).pow(2) + cos(phi1) * cos(phi2) * sin(dLambda / 2).pow(2)
        return r * 2 * asin(sqrt(a))
    }
}
