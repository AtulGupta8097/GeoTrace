package com.geofencing.tracker.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun AddGeofenceDialog(
    locationName: String,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var customName by remember { mutableStateOf(locationName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Geofence") },
        text = {
            Column {

                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text("Geofence Name (optional)") },
                    placeholder = { Text(locationName) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Radius: 100 meters",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(customName.takeIf { it.isNotBlank() })
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
