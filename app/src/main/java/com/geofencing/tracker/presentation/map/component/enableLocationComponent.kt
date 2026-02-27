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
