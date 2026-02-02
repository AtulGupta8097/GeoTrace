package com.geofencing.tracker.presentation.map

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.geofencing.tracker.presentation.components.AddGeofenceDialog
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val geofences by viewModel.geofences.collectAsState()

    val cameraPositionState = rememberCameraPositionState()
    var permissionGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        permissionGranted = granted.values.any { it }
    }

    LaunchedEffect(Unit) {
        if (!viewModel.hasLocationPermission()) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            )
        } else {
            permissionGranted = true
        }
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            viewModel.getCurrentLocation()?.let {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(it, 16f)
                )
            }
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
            onMapLongClick = viewModel::onMapLongClick
        ) {
            geofences.forEach { geofence ->
                Marker(
                    state = MarkerState(
                        position = LatLng(
                            geofence.latitude,
                            geofence.longitude
                        )
                    ),
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
            onClick = {
                viewModel.onMapLongClick(cameraPositionState.position.target)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text("Add Geofence")
        }
    }

    if (viewModel.showAddDialog) {
        AddGeofenceDialog(
            locationName = viewModel.selectedLocationName,
            onDismiss = viewModel::onDismissDialog,
            onConfirm = { customName ->
                viewModel.addGeofence(customName)
            }
        )
    }

    if (viewModel.shouldAskToEnableLocation) {
        AlertDialog(
            onDismissRequest = viewModel::onLocationDialogDismiss,
            title = { Text("Enable Location") },
            text = {
                Text("Location services are turned off. Please enable GPS to continue.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onLocationDialogDismiss()
                    context.startActivity(
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    )
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onLocationDialogDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}


