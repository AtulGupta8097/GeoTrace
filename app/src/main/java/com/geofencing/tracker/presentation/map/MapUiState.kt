package com.geofencing.tracker.presentation.map

import org.maplibre.android.geometry.LatLng

data class MapUiState(
    val selectedLocation: LatLng? = null,
    val selectedLocationName: String = "",
    val showAddDialog: Boolean = false,
    val shouldAskToEnableLocation: Boolean = false
)