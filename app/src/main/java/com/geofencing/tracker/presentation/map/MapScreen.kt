package com.geofencing.tracker.presentation.map

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.geofencing.tracker.presentation.components.AddGeofenceDialog
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel()
) {
    val geofences by viewModel.geofences.collectAsState()
    val defaultLocation = LatLng(8.524, 76.939)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 15f)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(Unit) {
        if (!viewModel.hasLocationPermission()) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = viewModel.hasLocationPermission()
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = true
            ),
            onMapLongClick = { latLng ->
                viewModel.onMapLongClick(latLng)
            }
        ) {
            geofences.forEach { geofence ->
                Marker(
                    state = MarkerState(position = LatLng(geofence.latitude, geofence.longitude)),
                    title = geofence.name
                )
                Circle(
                    center = LatLng(geofence.latitude, geofence.longitude),
                    radius = geofence.radius.toDouble(),
                    fillColor = androidx.compose.ui.graphics.Color(0x220000FF),
                    strokeColor = androidx.compose.ui.graphics.Color(0xFF0000FF),
                    strokeWidth = 2f
                )
            }
        }

        Button(
            onClick = { viewModel.onMapLongClick(cameraPositionState.position.target) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text("Add Geofence")
        }
    }

    if (viewModel.showAddDialog) {
        AddGeofenceDialog(
            onDismiss = { viewModel.onDismissDialog() },
            onConfirm = { name, radius ->
                viewModel.addGeofence(name, radius)
            }
        )
    }
}