package com.geofencing.tracker.presentation.map.component

import android.annotation.SuppressLint
import android.content.Context
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style

@SuppressLint("MissingPermission")
fun enableLocationComponent(
    context: Context,
    map: MapLibreMap,
    style: Style
) {
    try {
        map.locationComponent.apply {
            activateLocationComponent(
                LocationComponentActivationOptions.builder(context, style).build()
            )
            isLocationComponentEnabled = true
            cameraMode = CameraMode.NONE
            renderMode = RenderMode.COMPASS
        }
    } catch (_: Exception) {
    }
}

fun haversineMeters(
    lat1: Double,
    lng1: Double,
    lat2: Double,
    lng2: Double
): Double {
    val r = 6_371_000.0
    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val dPhi = Math.toRadians(lat2 - lat1)
    val dLambda = Math.toRadians(lng2 - lng1)

    val a = Math.sin(dPhi / 2).let { it * it } +
            Math.cos(phi1) * Math.cos(phi2) *
            Math.sin(dLambda / 2).let { it * it }

    return r * 2 * Math.asin(Math.sqrt(a))
}