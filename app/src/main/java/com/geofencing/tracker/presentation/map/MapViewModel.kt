package com.geofencing.tracker.presentation.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geofencing.tracker.domain.model.GeofenceLocation
import com.geofencing.tracker.domain.repository.GeofenceRepository
import com.geofencing.tracker.domain.usecase.AddGeofenceUseCase
import com.geofencing.tracker.domain.usecase.GetAllGeofencesUseCase
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import org.maplibre.android.geometry.LatLng
import com.google.android.gms.location.Priority
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
@HiltViewModel
class MapViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val addGeofenceUseCase: AddGeofenceUseCase,
    private val repository: GeofenceRepository,
    getAllGeofencesUseCase: GetAllGeofencesUseCase,
) : ViewModel() {

    val geofences = getAllGeofencesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState = _uiState.asStateFlow()

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun isLocationEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun getCurrentLocation(): LatLng? {
        if (!hasLocationPermission()) return null

        if (!isLocationEnabled()) {
            _uiState.update { it.copy(shouldAskToEnableLocation = true) }
            return null
        }

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

    fun onMapLongClick(latLng: LatLng) {
        viewModelScope.launch {
            val name = resolveLocationName(latLng)
            _uiState.update {
                it.copy(
                    selectedLocation = latLng,
                    selectedLocationName = name,
                    showAddDialog = true
                )
            }
        }
    }

    fun toggleGeofenceSelection(id: Long, currentlySelected: Boolean) {
        viewModelScope.launch {
            repository.toggleGeofenceSelection(id, !currentlySelected)
        }
    }

    fun dismissAddDialog() {
        _uiState.update { MapUiState() }
    }

    fun dismissLocationDialog() {
        _uiState.update { it.copy(shouldAskToEnableLocation = false) }
    }

    fun addGeofence(customName: String?) {
        val state = _uiState.value
        val location = state.selectedLocation ?: return

        viewModelScope.launch {
            val name = customName?.takeIf { it.isNotBlank() } ?: state.selectedLocationName
            addGeofenceUseCase(
                GeofenceLocation(
                    name = name,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    radius = 100f
                )
            )
            dismissAddDialog()
        }
    }

    private suspend fun resolveLocationName(latLng: LatLng): String =
        withContext(Dispatchers.IO) {
            try {
                Geocoder(context, Locale.getDefault())
                    .getFromLocation(latLng.latitude, latLng.longitude, 1)
                    ?.firstOrNull()
                    ?.featureName ?: "Selected Location"
            } catch (_: Exception) {
                "Selected Location"
            }
        }
}
