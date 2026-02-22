package com.geofencing.tracker.presentation.map

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
@Composable
fun MapScreen(
    onNavigateToRoute: () -> Unit = {},
    viewModel: MapViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val geofences by viewModel.geofences.collectAsStateWithLifecycle()

    val selectedCount = geofences.count { it.isSelected }

    // Permission state
    var permissionGranted by remember { mutableStateOf(viewModel.hasLocationPermission()) }
    var showPermissionRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionGranted =
            results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!permissionGranted) showPermissionRationale = true
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Map references
    val mapView = remember { MapView(context) }
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        mapView.onCreate(null)
        mapView.onStart()
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    // Redraw geofences when list changes
    LaunchedEffect(geofences, styleReady) {
        if (styleReady) {
            mapLibreMap?.let { drawGeofences(it, geofences) }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                mapView.apply {
                    getMapAsync { map ->
                        mapLibreMap = map
                        map.setStyle(
                            Style.Builder().fromUrl("https://tiles.openfreemap.org/styles/liberty")
                        ) { _ ->
                            styleReady = true

                            if (permissionGranted) {
                                scope.launch {
                                    val location = viewModel.getCurrentLocation()
                                    location?.let { latLng ->
                                        map.cameraPosition = CameraPosition.Builder()
                                            .target(latLng)
                                            .zoom(15.0)
                                            .build()
                                    }
                                }
                            }

                            // Long press → add new geofence
                            map.addOnMapLongClickListener { latLng ->
                                viewModel.onMapLongClick(latLng)
                                true
                            }

                            // Short tap → select/deselect nearest geofence
                            map.addOnMapClickListener { tapLatLng ->
                                val nearest = geofences.minByOrNull { fence ->
                                    haversineMeters(
                                        tapLatLng.latitude, tapLatLng.longitude,
                                        fence.latitude, fence.longitude
                                    )
                                }
                                nearest?.let { fence ->
                                    val dist = haversineMeters(
                                        tapLatLng.latitude, tapLatLng.longitude,
                                        fence.latitude, fence.longitude
                                    )
                                    // Only toggle if tap is within the geofence radius * 1.5 (generous hit area)
                                    if (dist <= fence.radius * 1.5) {
                                        viewModel.toggleGeofenceSelection(fence.id, fence.isSelected)
                                    }
                                }
                                false
                            }
                        }
                    }
                }
            },
            update = {}
        )

        // Loading spinner
        if (!styleReady) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        // Top info banner
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shadowElevation = 4.dp
            ) {
                Text(
                    text = if (geofences.isEmpty())
                        "Long-press map to add geofences"
                    else
                        "Tap a geofence to select • Long-press to add",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Bottom panel: legend + Start Route button
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Legend
            if (geofences.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendDot(Color(0xFF2196F3), "Normal")
                        LegendDot(Color(0xFFFF9800), "Selected ($selectedCount)")
                        LegendDot(Color(0xFF4CAF50), "Visited")
                    }
                }
            }

            // Start Route button — only show when at least 1 geofence is selected
            if (selectedCount > 0) {
                Button(
                    onClick = onNavigateToRoute,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.Navigation,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Start Route ($selectedCount stops)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }

    // Dialogs
    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = { Text("Location Permission Required") },
            text = { Text("This app needs location access to show your position and track geofences.") },
            confirmButton = {
                Button(onClick = {
                    showPermissionRationale = false
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                    })
                }) { Text("Open Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale = false }) { Text("Cancel") }
            }
        )
    }

    if (viewModel.shouldAskToEnableLocation) {
        AlertDialog(
            onDismissRequest = viewModel::onLocationDialogDismiss,
            title = { Text("Enable Location Services") },
            text = { Text("Location services are off. Please enable them.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.onLocationDialogDismiss()
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }) { Text("Open Settings") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onLocationDialogDismiss) { Text("Not Now") }
            }
        )
    }

    if (viewModel.showAddDialog) {
        AddGeofenceDialog(
            defaultName = viewModel.selectedLocationName,
            onConfirm = { name -> viewModel.addGeofence(name) },
            onDismiss = viewModel::onDismissDialog
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, shape = RoundedCornerShape(6.dp))
        )
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun AddGeofenceDialog(
    defaultName: String,
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(defaultName) { mutableStateOf(defaultName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Geofence") },
        text = {
            Column {
                Text("Long-pressed location:")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name.takeIf { it.isNotBlank() }) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// Haversine helper for tap detection (client-side only)
private fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6_371_000.0
    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val dPhi = Math.toRadians(lat2 - lat1)
    val dLambda = Math.toRadians(lng2 - lng1)
    val a = Math.sin(dPhi / 2).let { it * it } +
            Math.cos(phi1) * Math.cos(phi2) * Math.sin(dLambda / 2).let { it * it }
    return r * 2 * Math.asin(Math.sqrt(a))
}
