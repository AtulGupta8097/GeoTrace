package com.geofencing.tracker.presentation.map.component

import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun createCircle(
    centerLat: Double,
    centerLng: Double,
    radiusMeters: Double,
    points: Int = 64
): Polygon {

    val degLat = radiusMeters / 110_540.0
    val degLng = radiusMeters / (111_320.0 * cos(Math.toRadians(centerLat)))

    val ring = (0 until points).map { i ->
        val theta = 2.0 * PI * i / points
        Point.fromLngLat(
            centerLng + degLng * cos(theta),
            centerLat + degLat * sin(theta)
        )
    }.toMutableList()

    ring.add(ring.first())
    return Polygon.fromLngLats(listOf(ring))
}