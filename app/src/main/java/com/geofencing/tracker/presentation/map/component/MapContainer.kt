package com.geofencing.tracker.presentation.map.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import com.geofencing.tracker.domain.model.GeofenceLocation
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

@Composable
fun MapContainer(
    mapView: MapView,
    geofences: List<GeofenceLocation>,
    onMapReady: (MapLibreMap) -> Unit,
    onStyleReady: () -> Unit,
    onLongClick: (LatLng) -> Unit,
    onTap: (LatLng) -> Unit
) {
    AndroidView(factory = {
        mapView.apply {
            getMapAsync { map ->
                onMapReady(map)
                map.setStyle("https://tiles.openfreemap.org/styles/liberty") {
                    onStyleReady()

                    map.addOnMapLongClickListener {
                        onLongClick(it); true
                    }

                    map.addOnMapClickListener {
                        onTap(it); false
                    }
                }
            }
        }
    })
}