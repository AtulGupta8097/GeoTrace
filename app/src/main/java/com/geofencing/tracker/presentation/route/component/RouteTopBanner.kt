package com.geofencing.tracker.presentation.route.component

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.geofencing.tracker.presentation.route.RouteState

@Composable
fun BoxScope.RouteTopBanner(state: RouteState) {

    if (state.isFetchingRoute) {
        Surface(
            modifier = Modifier.align(Alignment.TopCenter).padding(8.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Row(Modifier.padding(12.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Fetching road route…")
            }
        }
    }

    if (!state.isFetchingRoute && state.routePoints.isNotEmpty() && state.nextStop != null) {

        val km = state.legDistanceMeters / 1000
        val mins = (state.legDurationSeconds / 60).toInt()

        Surface(
            modifier = Modifier.align(Alignment.TopCenter).padding(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                "🛣️ ${"%.1f".format(km)} km • 🕐 ${mins}m → ${state.nextStop!!.name}",
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}