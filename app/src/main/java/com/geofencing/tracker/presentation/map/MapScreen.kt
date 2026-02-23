package com.geofencing.tracker.presentation.map

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.geofencing.tracker.data.service.GeofenceService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
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

    var foregroundGranted by remember { mutableStateOf(viewModel.hasLocationPermission()) }
    var showRationale by remember { mutableStateOf(false) }

    val foregroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        foregroundGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!foregroundGranted) showRationale = true
    }

    val backgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    // Request foreground permissions on first launch
    LaunchedEffect(Unit) {
        if (!foregroundGranted) {
            foregroundLauncher.launch(
                buildList {
                    add(Manifest.permission.ACCESS_FINE_LOCATION)
                    add(Manifest.permission.ACCESS_COARSE_LOCATION)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }.toTypedArray()
            )
        }
    }

    // Once foreground is granted: request background location, then start the service
    LaunchedEffect(foregroundGranted) {
        if (foregroundGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                delay(600)
                backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            // Safe to start the foreground location service now that permission is held
            val intent = Intent(context, GeofenceService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

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

    LaunchedEffect(geofences, styleReady) {
        if (styleReady) mapLibreMap?.let { drawGeofences(it, geofences) }
    }

    LaunchedEffect(foregroundGranted, styleReady) {
        if (foregroundGranted && styleReady) {
            mapLibreMap?.style?.let { style ->
                mapLibreMap?.let { map ->
                    enableLocationComponent(context, map, style)
                    val location = viewModel.getCurrentLocation()
                    location?.let {
                        map.cameraPosition = CameraPosition.Builder().target(it).zoom(15.0).build()
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                mapView.apply {
                    getMapAsync { map ->
                        mapLibreMap = map
                        map.setStyle(Style.Builder().fromUrl("https://tiles.openfreemap.org/styles/liberty")) { style ->
                            styleReady = true

                            map.addOnMapLongClickListener { latLng ->
                                viewModel.onMapLongClick(latLng)
                                true
                            }

                            map.addOnMapClickListener { tapLatLng ->
                                geofences.minByOrNull { fence ->
                                    haversineMeters(tapLatLng.latitude, tapLatLng.longitude, fence.latitude, fence.longitude)
                                }?.let { fence ->
                                    val dist = haversineMeters(tapLatLng.latitude, tapLatLng.longitude, fence.latitude, fence.longitude)
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

        if (!styleReady) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shadowElevation = 4.dp
            ) {
                Text(
                    text = if (geofences.isEmpty()) "Long-press map to add geofences" else "Tap to select • Long-press to add",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (foregroundGranted && styleReady) {
            SmallFloatingActionButton(
                onClick = {
                    scope.launch {
                        viewModel.getCurrentLocation()?.let {
                            mapLibreMap?.cameraPosition = CameraPosition.Builder()
                                .target(it).zoom(16.0).build()
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = if (selectedCount > 0) 92.dp else 16.dp),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "My Location", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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

            if (selectedCount > 0) {
                Button(
                    onClick = onNavigateToRoute,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Start Route ($selectedCount stops)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text("Location Permission Required") },
            text = { Text("This app needs location access to show your position and track geofences.") },
            confirmButton = {
                Button(onClick = {
                    showRationale = false
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    )
                }) { Text("Open Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) { Text("Cancel") }
            }
        )
    }

    if (viewModel.shouldAskToEnableLocation) {
        AlertDialog(
            onDismissRequest = viewModel::onLocationDialogDismiss,
            title = { Text("Enable Location Services") },
            text = { Text("Please enable location services to use this feature.") },
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

@Suppress("MissingPermission")
private fun enableLocationComponent(context: Context, map: MapLibreMap, style: Style) {
    try {
        map.locationComponent.apply {
            activateLocationComponent(LocationComponentActivationOptions.builder(context, style).build())
            isLocationComponentEnabled = true
            cameraMode = CameraMode.NONE
            renderMode = RenderMode.COMPASS
        }
    } catch (_: Exception) {}
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(12.dp).background(color, shape = RoundedCornerShape(6.dp)))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun AddGeofenceDialog(defaultName: String, onConfirm: (String?) -> Unit, onDismiss: () -> Unit) {
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
            Button(onClick = { onConfirm(name.takeIf { it.isNotBlank() }) }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

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
