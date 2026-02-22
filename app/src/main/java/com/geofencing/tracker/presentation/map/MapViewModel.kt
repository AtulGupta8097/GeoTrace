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
import com.geofencing.tracker.domain.usecase.AddGeofenceUseCase
import com.geofencing.tracker.domain.usecase.GetAllGeofencesUseCase
import com.geofencing.tracker.domain.repository.GeofenceRepository
import com.google.android.gms.location.FusedLocationProviderClient
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

    // Permission / location helpers

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    fun isSystemLocationEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun getCurrentLocation(): LatLng? {
        if (!hasLocationPermission()) return null
        if (!isSystemLocationEnabled()) {
            shouldAskToEnableLocation = true
            return null
        }
        return try {
            val loc = fusedLocationClient.lastLocation.await()
            loc?.let { LatLng(it.latitude, it.longitude) }
        } catch (e: Exception) {
            null
        }
    }

    // Map interaction

    fun onMapLongClick(latLng: LatLng) {
        selectedLocation = latLng
        viewModelScope.launch {
            selectedLocationName = resolveLocationName(latLng)
            showAddDialog = true
        }
    }

    /** Toggle a geofence in/out of the route plan (tap on map circle) */
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
            val geofence = GeofenceLocation(
                name = name,
                latitude = location.latitude,
                longitude = location.longitude,
                radius = 100f
            )
            addGeofenceUseCase(geofence)
            onDismissDialog()
        }
    }

    // Private helpers

    private suspend fun resolveLocationName(latLng: LatLng): String =
        withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val address = geocoder
                    .getFromLocation(latLng.latitude, latLng.longitude, 1)
                    ?.firstOrNull()
                address?.featureName
                    ?: address?.locality
                    ?: address?.subAdminArea
                    ?: "Selected Location"
            } catch (e: Exception) {
                "Selected Location"
            }
        }
}
