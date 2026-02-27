package com.geofencing.tracker.presentation.route

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geofencing.tracker.presentation.map.component.rememberMapView
import com.geofencing.tracker.presentation.route.component.CenterLoader
import com.geofencing.tracker.presentation.route.component.CompletedState
import com.geofencing.tracker.presentation.route.component.EmptyState
import com.geofencing.tracker.presentation.route.component.RouteHeader
import com.geofencing.tracker.presentation.route.component.RouteStopItem
import com.geofencing.tracker.presentation.route.component.RouteTopBanner
import com.geofencing.tracker.presentation.route.component.drawCurrentLeg
import com.geofencing.tracker.presentation.route.component.enableLocationComponent
import com.geofencing.tracker.presentation.route.component.fitToCurrentLeg
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style

@Composable
fun RouteScreen(
    viewModel: RouteViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val mapView = rememberMapView()
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    LaunchedEffect(state.currentStopIndex) {
        if (state.orderedStops.isNotEmpty()) {
            listState.animateScrollToItem(
                state.currentStopIndex.coerceAtMost(state.orderedStops.lastIndex)
            )
        }
    }

    LaunchedEffect(state.routePoints, state.userLocation, state.currentStopIndex, styleReady) {
        if (styleReady) {
            map?.let { mapLibre ->
                state.nextStop?.let { next ->
                    drawCurrentLeg(mapLibre, state.routePoints, state.userLocation, next)
                    fitToCurrentLeg(mapLibre, state.userLocation, next)
                }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {

        Box(Modifier.fillMaxWidth().weight(1f)) {

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    mapView.apply {
                        getMapAsync { mapLibre ->
                            map = mapLibre
                            mapLibre.setStyle(
                                Style.Builder().fromUrl("https://tiles.openfreemap.org/styles/liberty")
                            ) { style ->
                                styleReady = true
                                enableLocationComponent(context, mapLibre, style)
                            }
                        }
                    }
                }
            )

            if (!styleReady) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }

            RouteTopBanner(state)
        }

        Column(
            Modifier.fillMaxWidth().weight(1f)
                .background(MaterialTheme.colorScheme.background)
        ) {

            RouteHeader(state, viewModel::loadRoute, viewModel::resetRoute)

            HorizontalDivider()

            state.error?.let {
                Surface(color = MaterialTheme.colorScheme.errorContainer) {
                    Text(it, Modifier.padding(12.dp))
                }
            }

            when {
                state.isLoading -> CenterLoader()
                state.orderedStops.isEmpty() -> EmptyState()
                state.allVisited -> CompletedState(viewModel::resetRoute)
                else -> LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(state.orderedStops) { index, stop ->
                        val isNext = index == state.currentStopIndex && !stop.isVisited
                        RouteStopItem(
                            index + 1,
                            stop,
                            if (isNext) state.distanceToNextMeters else null,
                            isNext
                        )
                    }
                }
            }
        }
    }
}
