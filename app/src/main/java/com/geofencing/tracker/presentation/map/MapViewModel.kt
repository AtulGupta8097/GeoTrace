package com.geofencing.tracker.presentation.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geofencing.tracker.domain.model.GeofenceLocation
import com.geofencing.tracker.domain.repository.GeofenceRepository
import com.geofencing.tracker.domain.usecase.AddGeofenceUseCase
import com.geofencing.tracker.domain.usecase.GetAllGeofencesUseCase
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.maplibre.android.geometry.LatLng
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

    var selectedLocation by mutableStateOf<LatLng?>(null)
        private set

    var selectedLocationName by mutableStateOf("")
        private set

    var showAddDialog by mutableStateOf(false)
        private set

    var shouldAskToEnableLocation by mutableStateOf(false)
        private set

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun isLocationEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun getCurrentLocation(): LatLng? {
        if (!hasLocationPermission()) return null
        if (!isLocationEnabled()) {
            shouldAskToEnableLocation = true
            return null
        }
        return try {
            // lastLocation can be null on first boot/install — fall back to a fresh fix
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
        } catch (e: Exception) {
            null
        }
    }

    fun onMapLongClick(latLng: LatLng) {
        selectedLocation = latLng
        viewModelScope.launch {
            selectedLocationName = resolveLocationName(latLng)
            showAddDialog = true
        }
    }

    fun toggleGeofenceSelection(geofenceId: Long, currentlySelected: Boolean) {
        viewModelScope.launch {
            repository.toggleGeofenceSelection(geofenceId, !currentlySelected)
        }
    }

    fun onDismissDialog() {
        showAddDialog = false
        selectedLocation = null
        selectedLocationName = ""
    }

    fun onLocationDialogDismiss() {
        shouldAskToEnableLocation = false
    }

    fun addGeofence(customName: String?) {
        val location = selectedLocation ?: return
        viewModelScope.launch {
            val name = customName?.takeIf { it.isNotBlank() } ?: selectedLocationName
            addGeofenceUseCase(GeofenceLocation(name = name, latitude = location.latitude, longitude = location.longitude, radius = 100f))
            onDismissDialog()
        }
    }

    private suspend fun resolveLocationName(latLng: LatLng): String = withContext(Dispatchers.IO) {
        try {
            @Suppress("DEPRECATION")
            Geocoder(context, Locale.getDefault())
                .getFromLocation(latLng.latitude, latLng.longitude, 1)
                ?.firstOrNull()
                ?.let { it.featureName ?: it.locality ?: it.subAdminArea }
                ?: "Selected Location"
        } catch (e: Exception) {
            "Selected Location"
        }
    }
}
