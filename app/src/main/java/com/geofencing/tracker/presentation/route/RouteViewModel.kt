package com.geofencing.tracker.presentation.route

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geofencing.tracker.domain.model.GeofenceLocation
import com.geofencing.tracker.domain.repository.GeofenceRepository
import com.google.android.gms.location.FusedLocationProviderClient
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.maplibre.android.geometry.LatLng
import javax.inject.Inject
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class RouteState(
    val orderedStops: List<GeofenceLocation> = emptyList(),
    val userLocation: LatLng? = null,
    val isLoading: Boolean = true,
    val allVisited: Boolean = false
)

@HiltViewModel
class RouteViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: GeofenceRepository,
    private val fusedLocationClient: FusedLocationProviderClient
) : ViewModel() {

    private val _state = MutableStateFlow(RouteState())
    val state: StateFlow<RouteState> = _state.asStateFlow()

    init {
        loadRoute()
    }

    fun loadRoute() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            val userLoc = getUserLocation()
            val selected = repository.getSelectedGeofences()

            val ordered = if (userLoc != null) {
                sortNearestFirst(userLoc.latitude, userLoc.longitude, selected)
            } else {
                selected
            }

            _state.value = RouteState(
                orderedStops = ordered,
                userLocation = userLoc,
                isLoading = false,
                allVisited = ordered.isNotEmpty() && ordered.all { it.isVisited }
            )
        }
    }

    fun resetRoute() {
        viewModelScope.launch {
            repository.clearAllSelections()
            repository.clearAllVisited()
            _state.value = RouteState(isLoading = false)
        }
    }

    /** Nearest-neighbour greedy sort starting from user position */
    private fun sortNearestFirst(
        startLat: Double,
        startLng: Double,
        stops: List<GeofenceLocation>
    ): List<GeofenceLocation> {
        if (stops.isEmpty()) return emptyList()

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
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return null
        return try {
            val loc = fusedLocationClient.lastLocation.await()
            loc?.let { LatLng(it.latitude, it.longitude) }
        } catch (e: Exception) {
            null
        }
    }

    fun distanceToStop(stop: GeofenceLocation): Double? {
        val loc = _state.value.userLocation ?: return null
        return haversineMeters(loc.latitude, loc.longitude, stop.latitude, stop.longitude)
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
