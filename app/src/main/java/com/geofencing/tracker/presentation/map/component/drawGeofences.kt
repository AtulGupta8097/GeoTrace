package com.geofencing.tracker.presentation.map.component

import com.geofencing.tracker.domain.model.GeofenceLocation
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.expressions.Expression.*
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection

private const val SOURCE_ID = "geofence-source"
private const val FILL_LAYER_ID = "geofence-fill"
private const val STROKE_LAYER_ID = "geofence-stroke"

fun drawGeofences(map: MapLibreMap, geofences: List<GeofenceLocation>) {
    val style = map.style ?: return

    // 🔹 Build polygon features with state property
    val features = geofences.map { fence ->
        Feature.fromGeometry(
            createCircle(fence.latitude, fence.longitude, fence.radius.toDouble())
        ).also { feature ->
            feature.addStringProperty(
                "state",
                when {
                    fence.isVisited -> "visited"
                    fence.isSelected -> "selected"
                    else -> "normal"
                }
            )
        }
    }

    val collection = FeatureCollection.fromFeatures(features)

    val source = style.getSourceAs<GeoJsonSource>(SOURCE_ID)
    if (source == null) {

        style.addSource(GeoJsonSource(SOURCE_ID, collection))

        // 🎨 Fill color per feature
        style.addLayer(
            FillLayer(FILL_LAYER_ID, SOURCE_ID).withProperties(
                fillColor(
                    match(
                        get("state"),
                        literal("visited"), literal("#4CAF50"),   // green
                        literal("selected"), literal("#FF9800"), // orange
                        literal("#2196F3")                        // blue
                    )
                ),
                fillOpacity(0.28f)
            )
        )

        // 🎨 Border color per feature
        style.addLayer(
            LineLayer(STROKE_LAYER_ID, SOURCE_ID).withProperties(
                lineColor(
                    match(
                        get("state"),
                        literal("visited"), literal("#2E7D32"),
                        literal("selected"), literal("#E65100"),
                        literal("#1565C0")
                    )
                ),
                lineWidth(2.5f)
            )
        )

    } else {
        source.setGeoJson(collection)
    }
}