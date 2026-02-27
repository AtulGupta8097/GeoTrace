package com.geofencing.tracker.presentation.route.component

import android.content.Context
import com.geofencing.tracker.domain.model.GeofenceLocation
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

private const val ROUTE_SOURCE_ID = "route-line-source"
private const val ROUTE_LAYER_ID = "route-line-layer"

fun drawCurrentLeg(
    map: MapLibreMap,
    roadPoints: List<Point>,
    userLocation: LatLng?,
    nextStop: GeofenceLocation
) {
    val style = map.style ?: return

    val points = roadPoints.ifEmpty {
        buildList {
            userLocation?.let { add(Point.fromLngLat(it.longitude, it.latitude)) }
            add(Point.fromLngLat(nextStop.longitude, nextStop.latitude))
        }
    }

    if (points.size >= 2) {
        val fc = FeatureCollection.fromFeatures(listOf(Feature.fromGeometry(LineString.fromLngLats(points))))
        val source = style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID)

        if (source == null) {
            style.addSource(GeoJsonSource(ROUTE_SOURCE_ID, fc))
            style.addLayer(
                LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                    lineColor("#1565C0"),
                    lineWidth(4f),
                    lineJoin("round"),
                    lineCap("round")
                )
            )
        } else source.setGeoJson(fc)
    }
}

fun fitToCurrentLeg(map: MapLibreMap, userLocation: LatLng?, nextStop: GeofenceLocation) {
    val pts = buildList {
        userLocation?.let { add(it) }
        add(LatLng(nextStop.latitude, nextStop.longitude))
    }
    if (pts.size < 2) return

    val bounds = LatLngBounds.Builder().also { pts.forEach { p -> it.include(p) } }.build()
    map.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100), 1000)
}

@Suppress("MissingPermission")
fun enableLocationComponent(context: Context, map: MapLibreMap, style: Style) {
    try {
        map.locationComponent.apply {
            activateLocationComponent(LocationComponentActivationOptions.builder(context, style).build())
            isLocationComponentEnabled = true
            cameraMode = CameraMode.NONE
            renderMode = RenderMode.COMPASS
        }
    } catch (_: Exception) {}
}