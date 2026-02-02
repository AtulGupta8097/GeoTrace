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
import com.geofencing.tracker.data.manager.GeofenceManager
import com.geofencing.tracker.domain.model.GeofenceLocation
import com.geofencing.tracker.domain.usecase.AddGeofenceUseCase
import com.geofencing.tracker.domain.usecase.GetAllGeofencesUseCase
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val addGeofenceUseCase: AddGeofenceUseCase,
    getAllGeofencesUseCase: GetAllGeofencesUseCase,
    private val geofenceManager: GeofenceManager
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

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isSystemLocationEnabled(): Boolean {
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun getCurrentLocation(): LatLng? {
        if (!hasLocationPermission()) return null

        if (!isSystemLocationEnabled()) {
            shouldAskToEnableLocation = true
            return null
        }

        return try {
            val fusedClient =
                LocationServices.getFusedLocationProviderClient(context)
            val location = fusedClient.lastLocation.await()
            location?.let { LatLng(it.latitude, it.longitude) }
        } catch (e: Exception) {
            null
        }
    }

    fun onMapLongClick(latLng: LatLng) {
        selectedLocation = latLng
        selectedLocationName = getLocationName(latLng)
        showAddDialog = true
    }

    fun onDismissDialog() {
        showAddDialog = false
        selectedLocation = null
    }

    fun onLocationDialogDismiss() {
        shouldAskToEnableLocation = false
    }

    fun addGeofence(customName: String?) {
        selectedLocation?.let { location ->
            viewModelScope.launch {
                val finalName = customName ?: selectedLocationName

                val geofence = GeofenceLocation(
                    name = finalName,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    radius = 100f
                )

                val id = addGeofenceUseCase(geofence)
                geofenceManager.addGeofence(geofence.copy(id = id))
                onDismissDialog()
            }
        }
    }

    private fun getLocationName(latLng: LatLng): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
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
