package com.geofencing.tracker.presentation.map.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TopHint(empty: Boolean) {
    Box(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(shape = RoundedCornerShape(12.dp), shadowElevation = 4.dp) {
            Text(
                if (empty) "Long-press map to add geofences"
                else "Tap to select • Long-press to add",
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}