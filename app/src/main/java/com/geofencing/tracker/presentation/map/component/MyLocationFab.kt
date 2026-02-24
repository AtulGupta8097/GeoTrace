package com.geofencing.tracker.presentation.map.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MyLocationFab(
    modifier: Modifier = Modifier,
    visible: Boolean,
    selectedCount: Int,
    onClick: () -> Unit
) {
    Box(modifier = modifier) {
        if (!visible) return

        SmallFloatingActionButton(
            onClick = onClick,
            modifier = Modifier
                .padding(end = 16.dp, bottom = if (selectedCount > 0) 92.dp else 16.dp)
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = null)
        }
    }

}