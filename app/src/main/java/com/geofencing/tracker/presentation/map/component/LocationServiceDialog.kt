package com.geofencing.tracker.presentation.map.component

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun LocationServiceDialog(
    context: Context,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enable Location Services") },
        text = { Text("Please enable location services to use this feature.") },
        confirmButton = {
            Button(onClick = {
                onDismiss()
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not Now")
            }
        }
    )
}