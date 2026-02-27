package com.geofencing.tracker.presentation.map

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geofencing.tracker.data.service.GeofenceService
import com.geofencing.tracker.presentation.map.component.AddGeofenceDialog
import com.geofencing.tracker.presentation.map.component.BottomPanel
import com.geofencing.tracker.presentation.map.component.LocationServiceDialog
import com.geofencing.tracker.presentation.map.component.MapContainer
import com.geofencing.tracker.presentation.map.component.MyLocationFab
import com.geofencing.tracker.presentation.map.component.PermissionDialog
import com.geofencing.tracker.presentation.map.component.TopHint
import com.geofencing.tracker.presentation.map.component.drawGeofences
import com.geofencing.tracker.presentation.map.component.enableLocationComponent
import com.geofencing.tracker.presentation.map.component.rememberMapView
import com.geofencing.tracker.utils.haversineMeters
import kotlinx.coroutines.launch
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.maps.MapLibreMap

@Composable
fun MapScreen(
    onNavigateToRoute: () -> Unit = {},
    viewModel: MapViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val geofences by viewModel.geofences.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val selectedCount = geofences.count { it.isSelected }

    var hasForegroundPermission by remember {
        mutableStateOf(viewModel.hasLocationPermission())
    }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val mapView = rememberMapView()
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember { mutableStateOf(false) }

    val foregroundPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    val notificationPermission =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.POST_NOTIFICATIONS
        else null

    val backgroundPermission = Manifest.permission.ACCESS_BACKGROUND_LOCATION

    val backgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        context.startForegroundService(Intent(context, GeofenceService::class.java))
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        backgroundPermission.let { backgroundLauncher.launch(it) }
    }

    val foregroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasForegroundPermission = result.values.any { it }

        if (!hasForegroundPermission) {
            showPermissionDialog = true
        } else {
            notificationPermission?.let { notificationLauncher.launch(it) }
                ?: backgroundLauncher.launch(backgroundPermission)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasForegroundPermission) {
            foregroundLauncher.launch(foregroundPermissions)
        } else {
            notificationPermission?.let { notificationLauncher.launch(it) }
        }
    }

    LaunchedEffect(geofences, styleReady) {
        if (styleReady) {
            map?.let { drawGeofences(it, geofences) }
        }
    }

    Box(Modifier.fillMaxSize()) {

        MapContainer(
            mapView = mapView,
            geofences = geofences,
            onMapReady = { map = it },
            onStyleReady = { styleReady = true },
            onLongClick = viewModel::onMapLongClick,
            onTap = { latLng ->
                geofences.minByOrNull { fence ->
                    haversineMeters(
                        latLng.latitude,
                        latLng.longitude,
                        fence.latitude,
                        fence.longitude
                    )
                }?.let { fence ->
                    val dist = haversineMeters(
                        latLng.latitude,
                        latLng.longitude,
                        fence.latitude,
                        fence.longitude
                    )
                    if (dist <= fence.radius * 1.5) {
                        viewModel.toggleGeofenceSelection(fence.id, fence.isSelected)
                    }
                }
            }
        )

        LaunchedEffect(hasForegroundPermission, styleReady) {
            if (hasForegroundPermission && styleReady) {
                map?.style?.let { style ->
                    map?.let { enableLocationComponent(context, it, style) }
                }

                viewModel.getCurrentLocation()?.let {
                    map?.cameraPosition =
                        CameraPosition.Builder()
                            .target(it)
                            .zoom(15.0)
                            .build()
                }
            }
        }

        if (!styleReady) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }

        TopHint(geofences.isEmpty())

        MyLocationFab(
            modifier = Modifier.align(Alignment.BottomEnd),
            visible = hasForegroundPermission && styleReady,
            selectedCount = selectedCount,
            onClick = {
                if (hasForegroundPermission) {
                    scope.launch {
                        viewModel.getCurrentLocation()?.let {
                            map?.cameraPosition =
                                CameraPosition.Builder()
                                    .target(it)
                                    .zoom(16.0)
                                    .build()
                        }
                    }
                }
            }
        )

        BottomPanel(
            modifier = Modifier.align(Alignment.BottomCenter),
            geofencesExist = geofences.isNotEmpty(),
            selectedCount = selectedCount,
            onNavigate = onNavigateToRoute
        )
    }

    PermissionDialog(
        show = showPermissionDialog,
        context = context
    ) {
        showPermissionDialog = false
    }

    if (uiState.shouldAskToEnableLocation) {
        LocationServiceDialog(context, viewModel::dismissLocationDialog)
    }

    if (uiState.showAddDialog) {
        AddGeofenceDialog(
            defaultName = uiState.selectedLocationName,
            onConfirm = { viewModel.addGeofence(it) },
            onDismiss = viewModel::dismissAddDialog
        )
    }
}
