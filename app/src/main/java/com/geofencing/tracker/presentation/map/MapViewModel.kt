package com.geofencing.tracker.presentation.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geofencing.tracker.data.manager.GeofenceManager
import com.geofencing.tracker.domain.model.GeofenceLocation
import com.geofencing.tracker.domain.usecase.AddGeofenceUseCase
import com.geofencing.tracker.domain.usecase.GetAllGeofencesUseCase
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val addGeofenceUseCase: AddGeofenceUseCase,
    getAllGeofencesUseCase: GetAllGeofencesUseCase,
    private val geofenceManager: GeofenceManager
) : ViewModel() {

    val geofences = getAllGeofencesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var showAddDialog by mutableStateOf(false)
        private set

    var selectedLocation by mutableStateOf<LatLng?>(null)
        private set

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun onMapLongClick(latLng: LatLng) {
        selectedLocation = latLng
        showAddDialog = true
    }

    fun onDismissDialog() {
        showAddDialog = false
        selectedLocation = null
    }

    fun addGeofence(name: String, radius: Float) {
        selectedLocation?.let { location ->
            viewModelScope.launch {
                val geofence = GeofenceLocation(
                    name = name,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    radius = radius
                )
                val id = addGeofenceUseCase(geofence)
                geofenceManager.addGeofence(geofence.copy(id = id))
                onDismissDialog()
            }
        }
    }
}