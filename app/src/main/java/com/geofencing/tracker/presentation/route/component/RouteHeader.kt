package com.geofencing.tracker.presentation.route.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.geofencing.tracker.presentation.route.RouteState

@Composable
fun RouteHeader(
    state: RouteState,
    onRefresh: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Route Stops", style = MaterialTheme.typography.titleLarge)
            if (!state.isLoading && state.orderedStops.isNotEmpty()) {
                Text("${state.visitedCount} / ${state.orderedStops.size} visited")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, null)
            }
            if (state.orderedStops.isNotEmpty()) {
                OutlinedButton(onClick = onReset) { Text("Clear") }
            }
        }
    }
}