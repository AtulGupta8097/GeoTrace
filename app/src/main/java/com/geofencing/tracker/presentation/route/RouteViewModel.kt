package com.geofencing.tracker.presentation.route

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geofencing.tracker.domain.model.GeofenceLocation
import com.geofencing.tracker.domain.repository.GeofenceRepository
import com.geofencing.tracker.utils.haversineMeters
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
import kotlinx.coroutines.flow.update
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
    val nextStop: GeofenceLocation?
        get() = orderedStops.getOrNull(currentStopIndex)

    val visitedCount: Int
        get() = orderedStops.count { it.isVisited }
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

    init {
        loadRoute()
    }

    fun loadRoute() {
        viewModelScope.launch {
            _state.value = RouteState(isLoading = true)

            val userLoc = getUserLocation()
            val selected = repository.getSelectedGeofences()

            if (selected.isEmpty()) {
                _state.value = RouteState(isLoading = false, userLocation = userLoc)
                return@launch
            }

            val ordered = userLoc?.let {
                sortNearestFirst(it.latitude, it.longitude, selected)
            } ?: selected

            val startIndex = ordered.indexOfFirst { !it.isVisited }
                .takeIf { it >= 0 } ?: 0

            _state.value = RouteState(
                orderedStops = ordered,
                currentStopIndex = startIndex,
                userLocation = userLoc,
                isLoading = false,
                isFetchingRoute = userLoc != null,
                allVisited = ordered.all { it.isVisited }
            )

            ordered.getOrNull(startIndex)?.let { next ->
                if (userLoc != null && !ordered.all { it.isVisited }) {
                    fetchLeg(userLoc, next)
                }
            }

            startLocationPolling()
        }
    }

    fun resetRoute() {
        viewModelScope.launch {
            locationPollJob?.cancel()
            repository.clearAllSelections()
            repository.clearAllVisited()
            _state.value = RouteState(isLoading = false)
        }
    }

    override fun onCleared() {
        locationPollJob?.cancel()
        super.onCleared()
    }

    private fun startLocationPolling() {
        locationPollJob?.cancel()

        locationPollJob = viewModelScope.launch {
            while (isActive) {
                val loc = getUserLocation()
                val s = _state.value

                if (loc != null && !s.allVisited) {
                    s.nextStop?.let { next ->
                        val dist = haversineMeters(
                            loc.latitude, loc.longitude,
                            next.latitude, next.longitude
                        )

                        _state.value = s.copy(
                            userLocation = loc,
                            distanceToNextMeters = dist
                        )

                        if (dist <= next.radius) {
                            onArrived(next, loc)
                        }
                    }
                }

                delay(5_000)
            }
        }
    }

    private suspend fun onArrived(stop: GeofenceLocation, userLoc: LatLng) {
        repository.markGeofenceVisited(stop.id, true)

        val s = _state.value
        val updatedStops = s.orderedStops.map {
            if (it.id == stop.id) it.copy(isVisited = true) else it
        }

        val nextIndex = s.currentStopIndex + 1
        val allDone = nextIndex >= updatedStops.size || updatedStops.all { it.isVisited }

        if (allDone) {
            _state.value = s.copy(
                orderedStops = updatedStops,
                currentStopIndex = nextIndex.coerceAtMost(updatedStops.size),
                routePoints = emptyList(),
                allVisited = true,
                distanceToNextMeters = null
            )
            return
        }

        val nextStop = updatedStops[nextIndex]

        _state.value = s.copy(
            orderedStops = updatedStops,
            currentStopIndex = nextIndex,
            userLocation = userLoc,
            routePoints = emptyList(),
            isFetchingRoute = true,
            distanceToNextMeters = haversineMeters(
                userLoc.latitude, userLoc.longitude,
                nextStop.latitude, nextStop.longitude
            )
        )

        fetchLeg(userLoc, nextStop)
    }

    private suspend fun fetchLeg(from: LatLng, to: GeofenceLocation) {
        _state.update { it.copy(isFetchingRoute = true, error = null) }

        try {
            val coords = "${from.longitude},${from.latitude};${to.longitude},${to.latitude}"
            val url =
                "https://router.project-osrm.org/route/v1/driving/$coords?overview=full&geometries=geojson"

            val json = withContext(Dispatchers.IO) {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 10_000
                conn.readTimeout = 15_000

                try {
                    if (conn.responseCode == 200)
                        conn.inputStream.bufferedReader().readText()
                    else null
                } finally {
                    conn.disconnect()
                }
            }

            if (json == null) {
                _state.update { it.copy(isFetchingRoute = false, error = "Network error") }
                return
            }

            val root = JSONObject(json)
            if (root.optString("code") != "Ok") {
                _state.update { it.copy(isFetchingRoute = false, error = "Routing error") }
                return
            }

            val route = root.getJSONArray("routes").getJSONObject(0)
            val coordsArr = route.getJSONObject("geometry").getJSONArray("coordinates")

            val points = (0 until coordsArr.length()).map { i ->
                val pair = coordsArr.getJSONArray(i)
                Point.fromLngLat(pair.getDouble(0), pair.getDouble(1))
            }

            _state.update {
                it.copy(
                    routePoints = points,
                    legDistanceMeters = route.getDouble("distance"),
                    legDurationSeconds = route.getDouble("duration"),
                    isFetchingRoute = false
                )
            }

        } catch (e: Exception) {
            _state.update {
                it.copy(isFetchingRoute = false, error = e.message)
            }
        }
    }

    private fun sortNearestFirst(
        startLat: Double,
        startLng: Double,
        stops: List<GeofenceLocation>
    ): List<GeofenceLocation> {
        val remaining = stops.toMutableList()
        val result = mutableListOf<GeofenceLocation>()

        var curLat = startLat
        var curLng = startLng

        while (remaining.isNotEmpty()) {
            val next = remaining.minByOrNull {
                haversineMeters(curLat, curLng, it.latitude, it.longitude)
            }!!
            result.add(next)
            remaining.remove(next)
            curLat = next.latitude
            curLng = next.longitude
        }

        return result
    }

    private suspend fun getUserLocation(): LatLng? {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return null

        return try {
            val last = fusedLocationClient.lastLocation.await()

            last?.let { LatLng(it.latitude, it.longitude) }
                ?: fusedLocationClient.getCurrentLocation(
                    CurrentLocationRequest.Builder()
                        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                        .build(),
                    null
                ).await()?.let { LatLng(it.latitude, it.longitude) }

        } catch (_: Exception) {
            null
        }
    }
}
