package com.geofencing.tracker.presentation.route.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.geofencing.tracker.domain.model.GeofenceLocation

@Composable
fun RouteStopItem(
    index: Int,
    stop: GeofenceLocation,
    distanceMeters: Double?,
    isNext: Boolean
) {
    val bgColor = when {
        stop.isVisited -> Color(0xFFE8F5E9)
        isNext -> Color(0xFFFFF3E0)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                Modifier.size(38.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(index.toString(), color = Color.White)
            }

            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(stop.name, fontWeight = FontWeight.SemiBold)
                distanceMeters?.let {
                    val text = if (it >= 1000) "${"%.1f".format(it / 1000)} km" else "${it.toInt()} m"
                    Text("$text away • Radius ${stop.radius.toInt()} m")
                }
            }

            if (stop.isVisited)
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50))
            else
                Icon(Icons.Default.RadioButtonUnchecked, null)
        }
    }
}