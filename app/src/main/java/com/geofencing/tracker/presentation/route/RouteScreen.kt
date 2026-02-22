package com.geofencing.tracker.presentation.route

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
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
import com.geofencing.tracker.domain.model.GeofenceLocation
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

@Composable
fun RouteScreen(
    viewModel: RouteViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

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

    // Draw route on map whenever state changes
    LaunchedEffect(state, styleReady) {
        if (styleReady && state.orderedStops.isNotEmpty()) {
            mapLibreMap?.let { map ->
                drawRouteOnMap(map, state.orderedStops, state.userLocation)
                fitMapToRoute(map, state.orderedStops, state.userLocation)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Map (top half) ────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    mapView.apply {
                        getMapAsync { map ->
                            mapLibreMap = map
                            map.setStyle(
                                Style.Builder().fromUrl("https://tiles.openfreemap.org/styles/liberty")
                            ) { styleReady = true }
                        }
                    }
                },
                update = {}
            )

            if (!styleReady) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }

        // ── Stop list (bottom half) ───────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Route Plan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (!state.isLoading) {
                        val visited = state.orderedStops.count { it.isVisited }
                        Text(
                            "$visited / ${state.orderedStops.size} visited",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { viewModel.loadRoute() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    if (state.orderedStops.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { viewModel.resetRoute() },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Clear Route")
                        }
                    }
                }
            }

            HorizontalDivider()

            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                state.orderedStops.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No geofences selected", style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Tap geofences on the Map tab to add them to your route",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                state.allVisited -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🎉", fontSize = 48.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "All stops visited!",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { viewModel.resetRoute() }) {
                                Text("Start New Route")
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        itemsIndexed(state.orderedStops) { index, stop ->
                            RouteStopItem(
                                index = index + 1,
                                stop = stop,
                                distanceMeters = viewModel.distanceToStop(stop),
                                isNext = !stop.isVisited && state.orderedStops
                                    .take(index)
                                    .all { it.isVisited }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteStopItem(
    index: Int,
    stop: GeofenceLocation,
    distanceMeters: Double?,
    isNext: Boolean
) {
    val bgColor = when {
        stop.isVisited -> Color(0xFFE8F5E9)
        isNext -> Color(0xFFFFF3E0)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(if (isNext) 4.dp else 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stop number badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = when {
                            stop.isVisited -> Color(0xFF4CAF50)
                            isNext -> Color(0xFFFF9800)
                            else -> MaterialTheme.colorScheme.primary
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = index.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stop.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isNext) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFF9800)
                        ) {
                            Text(
                                "NEXT",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))

                distanceMeters?.let { dist ->
                    val distText = if (dist >= 1000) "%.1f km".format(dist / 1000) else "${dist.toInt()} m"
                    Text(
                        text = "$distText away • Radius: ${stop.radius.toInt()} m",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Visited / not visited icon
            if (stop.isVisited) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Visited",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Icon(
                    Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Not visited",
                    tint = if (isNext) Color(0xFFFF9800) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// ── Map drawing helpers ───────────────────────────────────────────────────────

private const val ROUTE_SOURCE_ID = "route-source"
private const val ROUTE_LAYER_ID = "route-layer"
private const val STOPS_SOURCE_ID = "stops-source"
private const val STOPS_LAYER_ID = "stops-layer"

private fun drawRouteOnMap(
    map: MapLibreMap,
    stops: List<GeofenceLocation>,
    userLocation: LatLng?
) {
    val style = map.style ?: return

    // Build route line: user → stop1 → stop2 → ...
    val routePoints = mutableListOf<Point>()
    userLocation?.let { routePoints.add(Point.fromLngLat(it.longitude, it.latitude)) }
    stops.forEach { routePoints.add(Point.fromLngLat(it.longitude, it.latitude)) }

    val lineFeature = Feature.fromGeometry(LineString.fromLngLats(routePoints))
    val lineCollection = FeatureCollection.fromFeatures(listOf(lineFeature))

    val existingRouteSource = style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID)
    if (existingRouteSource == null) {
        style.addSource(GeoJsonSource(ROUTE_SOURCE_ID, lineCollection))
        style.addLayer(
            LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                lineColor("#FF9800"),
                lineWidth(3.5f),
                lineDasharray(arrayOf(2f, 1f))
            )
        )
    } else {
        existingRouteSource.setGeoJson(lineCollection)
    }

    // Stop markers with numbers
    val stopFeatures = stops.mapIndexed { index, stop ->
        Feature.fromGeometry(
            Point.fromLngLat(stop.longitude, stop.latitude)
        ).also { f ->
            val label = if (stop.isVisited) "✅" else "${index + 1}"
            f.addStringProperty("label", label)
        }
    }
    val stopsCollection = FeatureCollection.fromFeatures(stopFeatures)

    val existingStopsSource = style.getSourceAs<GeoJsonSource>(STOPS_SOURCE_ID)
    if (existingStopsSource == null) {
        style.addSource(GeoJsonSource(STOPS_SOURCE_ID, stopsCollection))
        style.addLayer(
            SymbolLayer(STOPS_LAYER_ID, STOPS_SOURCE_ID).withProperties(
                textField("{label}"),
                textSize(16f),
                textColor("#E65100"),
                textAnchor("bottom")
            )
        )
    } else {
        existingStopsSource.setGeoJson(stopsCollection)
    }
}

private fun fitMapToRoute(
    map: MapLibreMap,
    stops: List<GeofenceLocation>,
    userLocation: LatLng?
) {
    val allPoints = mutableListOf<LatLng>()
    userLocation?.let { allPoints.add(it) }
    stops.forEach { allPoints.add(LatLng(it.latitude, it.longitude)) }

    if (allPoints.isEmpty()) return

    if (allPoints.size == 1) {
        map.cameraPosition = CameraPosition.Builder()
            .target(allPoints.first())
            .zoom(15.0)
            .build()
        return
    }

    val boundsBuilder = LatLngBounds.Builder()
    allPoints.forEach { boundsBuilder.include(it) }
    val bounds = boundsBuilder.build()
    map.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, 80), 1000)
}
