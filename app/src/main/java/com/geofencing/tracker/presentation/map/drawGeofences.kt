package com.geofencing.tracker.presentation.map

import com.geofencing.tracker.domain.model.GeofenceLocation
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.expressions.Expression.*
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

private const val SOURCE_ID = "geofence-source"
private const val FILL_LAYER_ID = "geofence-fill"
private const val STROKE_LAYER_ID = "geofence-stroke"
private const val LABEL_SOURCE_ID = "geofence-label-source"
private const val LABEL_LAYER_ID = "geofence-label"

/**
 * Draws all geofences with color coding:
 *  - Blue   = normal (not selected)
 *  - Orange = selected for route
 *  - Green  = visited during route
 */
fun drawGeofences(
    map: MapLibreMap,
    geofences: List<GeofenceLocation>
) {
    val style = map.style ?: return

    // Build circle polygon features with state properties
    val circleFeatures = geofences.map { fence ->
        Feature.fromGeometry(
            createCircle(fence.latitude, fence.longitude, fence.radius.toDouble())
        ).also { f ->
            f.addStringProperty("id", fence.id.toString())
            f.addStringProperty(
                "state",
                when {
                    fence.isVisited -> "visited"
                    fence.isSelected -> "selected"
                    else -> "normal"
                }
            )
        }
    }
    val circleCollection = FeatureCollection.fromFeatures(circleFeatures)

    val existingCircleSource = style.getSourceAs<GeoJsonSource>(SOURCE_ID)
    if (existingCircleSource == null) {
        style.addSource(GeoJsonSource(SOURCE_ID, circleCollection))

        // Fill colour driven by "state" property
        style.addLayer(
            FillLayer(FILL_LAYER_ID, SOURCE_ID).withProperties(
                fillColor(
                    match(
                        get("state"),
                        literal("visited"), literal("#4CAF50"),   // green
                        literal("selected"), literal("#FF9800"),  // orange
                        literal("#2196F3")                         // blue (default)
                    )
                ),
                fillOpacity(0.30f)
            )
        )

        style.addLayer(
            LineLayer(STROKE_LAYER_ID, SOURCE_ID).withProperties(
                lineColor(
                    match(
                        get("state"),
                        literal("visited"), literal("#2E7D32"),   // dark green
                        literal("selected"), literal("#E65100"),  // dark orange
                        literal("#1565C0")                         // dark blue (default)
                    )
                ),
                lineWidth(2.5f)
            )
        )
    } else {
        existingCircleSource.setGeoJson(circleCollection)
    }

    // Label features
    val labelFeatures = geofences.mapIndexed { index, fence ->
        Feature.fromGeometry(
            Point.fromLngLat(fence.longitude, fence.latitude)
        ).also { feature ->
            val prefix = when {
                fence.isVisited -> "✅ "
                fence.isSelected -> "📍 "
                else -> ""
            }
            feature.addStringProperty("name", "$prefix${fence.name}")
        }
    }
    val labelCollection = FeatureCollection.fromFeatures(labelFeatures)

    val existingLabelSource = style.getSourceAs<GeoJsonSource>(LABEL_SOURCE_ID)
    if (existingLabelSource == null) {
        style.addSource(GeoJsonSource(LABEL_SOURCE_ID, labelCollection))
        style.addLayer(
            SymbolLayer(LABEL_LAYER_ID, LABEL_SOURCE_ID).withProperties(
                textField("{name}"),
                textColor("#0D47A1"),
                textSize(13f),
                textAnchor("top")
            )
        )
    } else {
        existingLabelSource.setGeoJson(labelCollection)
    }
}
