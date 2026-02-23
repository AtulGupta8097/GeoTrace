package com.geofencing.tracker.presentation.route

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geofencing.tracker.domain.model.GeofenceLocation
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.PropertyFactory.textAnchor
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textOffset
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

@Composable
fun RouteScreen(viewModel: RouteViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val mapView = remember { MapView(context) }
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(state.currentStopIndex) {
        if (state.orderedStops.isNotEmpty()) {
            listState.animateScrollToItem(state.currentStopIndex.coerceAtMost(state.orderedStops.lastIndex))
        }
    }

    DisposableEffect(Unit) {
        mapView.onCreate(null); mapView.onStart(); mapView.onResume()
        onDispose { mapView.onPause(); mapView.onStop(); mapView.onDestroy() }
    }

    LaunchedEffect(state.routePoints, state.userLocation, state.currentStopIndex, styleReady) {
        if (styleReady) {
            mapLibreMap?.let { map ->
                val nextStop = state.nextStop
                if (nextStop != null) {
                    drawCurrentLeg(map, state.routePoints, state.userLocation, nextStop)
                    fitToCurrentLeg(map, state.userLocation, nextStop)
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    mapView.apply {
                        getMapAsync { map ->
                            mapLibreMap = map
                            map.setStyle(Style.Builder().fromUrl("https://tiles.openfreemap.org/styles/liberty")) { style ->
                                styleReady = true
                                enableLocationComponent(context, map, style)
                            }
                        }
                    }
                },
                update = {}
            )

            if (!styleReady) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            if (state.isFetchingRoute) {
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Fetching road route…", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (!state.isFetchingRoute && state.routePoints.isNotEmpty() && state.nextStop != null) {
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
                ) {
                    val km = state.legDistanceMeters / 1000
                    val mins = (state.legDurationSeconds / 60).toInt()
                    val durationText = if (mins >= 60) "${mins / 60}h ${mins % 60}m" else "${mins}m"
                    Text(
                        text = "🛣️  ${"%.1f".format(km)} km  •  🕐 $durationText  →  ${state.nextStop!!.name}",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).background(MaterialTheme.colorScheme.background)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Route Stops", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (!state.isLoading && state.orderedStops.isNotEmpty()) {
                        Text(
                            "${state.visitedCount} / ${state.orderedStops.size} visited",
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
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) { Text("Clear") }
                    }
                }
            }

            HorizontalDivider()

            state.error?.let { err ->
                Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(err, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.orderedStops.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No stops selected", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(8.dp))
                        Text("Tap geofences on the Map tab to add them", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                state.allVisited -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎉", fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("All stops visited!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.resetRoute() }) { Text("Start New Route") }
                    }
                }
                else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
                    itemsIndexed(state.orderedStops) { index, stop ->
                        val isCurrentTarget = index == state.currentStopIndex && !stop.isVisited
                        RouteStopItem(
                            index = index + 1,
                            stop = stop,
                            distanceMeters = if (isCurrentTarget) state.distanceToNextMeters else null,
                            isNext = isCurrentTarget
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteStopItem(index: Int, stop: GeofenceLocation, distanceMeters: Double?, isNext: Boolean) {
    val bgColor = when {
        stop.isVisited -> Color(0xFFE8F5E9)
        isNext -> Color(0xFFFFF3E0)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(if (isNext) 4.dp else 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(38.dp).background(
                    color = when {
                        stop.isVisited -> Color(0xFF4CAF50)
                        isNext -> Color(0xFFFF9800)
                        else -> MaterialTheme.colorScheme.primary
                    },
                    shape = CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(index.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stop.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (isNext) {
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFF9800)) {
                            Text("NEXT", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                distanceMeters?.let { dist ->
                    val distText = if (dist >= 1000) "${"%.1f".format(dist / 1000)} km" else "${dist.toInt()} m"
                    Text("$distText away • Radius: ${stop.radius.toInt()} m", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (stop.isVisited) {
                Icon(Icons.Default.CheckCircle, "Visited", tint = Color(0xFF4CAF50), modifier = Modifier.size(28.dp))
            } else {
                Icon(Icons.Default.RadioButtonUnchecked, "Not visited", tint = if (isNext) Color(0xFFFF9800) else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
            }
        }
    }
}

private const val ROUTE_SOURCE_ID = "route-line-source"
private const val ROUTE_LAYER_ID = "route-line-layer"
private const val STOPS_SOURCE_ID = "route-stops-source"
private const val STOPS_LAYER_ID = "route-stops-layer"

private fun drawCurrentLeg(map: MapLibreMap, roadPoints: List<Point>, userLocation: LatLng?, nextStop: GeofenceLocation) {
    val style = map.style ?: return

    val linePoints: List<Point> = roadPoints.ifEmpty {
        buildList {
            userLocation?.let { add(Point.fromLngLat(it.longitude, it.latitude)) }
            add(Point.fromLngLat(nextStop.longitude, nextStop.latitude))
        }
    }

    if (linePoints.size >= 2) {
        val collection = FeatureCollection.fromFeatures(listOf(Feature.fromGeometry(LineString.fromLngLats(linePoints))))
        val existing = style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID)
        if (existing == null) {
            style.addSource(GeoJsonSource(ROUTE_SOURCE_ID, collection))
            style.addLayer(
                LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                    lineColor(if (roadPoints.isNotEmpty()) "#1565C0" else "#9E9E9E"),
                    lineWidth(4f), lineJoin("round"), lineCap("round")
                )
            )
        } else {
            existing.setGeoJson(collection)
            style.getLayerAs<LineLayer>(ROUTE_LAYER_ID)?.setProperties(lineColor(if (roadPoints.isNotEmpty()) "#1565C0" else "#9E9E9E"))
        }
    }

    val stopFeature = Feature.fromGeometry(Point.fromLngLat(nextStop.longitude, nextStop.latitude))
        .also { it.addStringProperty("name", nextStop.name) }
    val existingStops = style.getSourceAs<GeoJsonSource>(STOPS_SOURCE_ID)
    if (existingStops == null) {
        style.addSource(GeoJsonSource(STOPS_SOURCE_ID, FeatureCollection.fromFeatures(listOf(stopFeature))))
        style.addLayer(
            SymbolLayer(STOPS_LAYER_ID, STOPS_SOURCE_ID).withProperties(
                textField("{name}"), textSize(13f), textColor("#0D47A1"),
                textAnchor("top"), textOffset(arrayOf(0f, 0.5f))
            )
        )
    } else {
        existingStops.setGeoJson(FeatureCollection.fromFeatures(listOf(stopFeature)))
    }
}

private fun fitToCurrentLeg(map: MapLibreMap, userLocation: LatLng?, nextStop: GeofenceLocation) {
    val points = buildList {
        userLocation?.let { add(it) }
        add(LatLng(nextStop.latitude, nextStop.longitude))
    }
    if (points.size < 2) return
    val builder = LatLngBounds.Builder()
    points.forEach { builder.include(it) }
    map.easeCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100), 1000)
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
