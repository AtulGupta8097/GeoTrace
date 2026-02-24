package com.geofencing.tracker.presentation.map.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BottomPanel(
    modifier: Modifier = Modifier,
    geofencesExist: Boolean,
    selectedCount: Int,
    onNavigate: () -> Unit
) {
    Box(modifier = modifier ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (geofencesExist) {
                Text("Selected: $selectedCount")
            }
            if (selectedCount > 0) {
                Button(onClick = onNavigate, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Navigation, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start Route")
                }
            }
        }
    }
    }
