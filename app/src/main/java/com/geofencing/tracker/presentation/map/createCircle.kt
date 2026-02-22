package com.geofencing.tracker.presentation.map

import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Builds a GeoJSON [Polygon] approximating a circle.
 *
 * The ring is explicitly closed (first == last point) as required by the GeoJSON spec.
 *
 * @param centerLat  Latitude of the circle centre (degrees)
 * @param centerLng  Longitude of the circle centre (degrees)
 * @param radiusMeters  Radius in metres
 * @param points  Number of polygon vertices (higher = smoother, default 64)
 */
fun createCircle(
    centerLat: Double,
    centerLng: Double,
    radiusMeters: Double,
    points: Int = 64
): Polygon {
    // Degrees-per-metre varies with latitude; compute both axes independently
    val degLng = radiusMeters / (111_320.0 * cos(Math.toRadians(centerLat)))
    val degLat = radiusMeters / 110_540.0

    // Build the outer ring: 0 until points (exclusive) → exactly `points` vertices
    val ring = (0 until points).map { i ->
        val theta = 2.0 * PI * i / points
        Point.fromLngLat(
            centerLng + degLng * cos(theta),
            centerLat + degLat * sin(theta)
        )
    }.toMutableList()

    // Close the ring by repeating the first point at the end
    ring.add(ring.first())

    return Polygon.fromLngLats(listOf(ring))
}