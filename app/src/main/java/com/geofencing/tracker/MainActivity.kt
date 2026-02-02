package com.geofencing.tracker

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import com.geofencing.tracker.presentation.main_screen.MainScreen
import com.geofencing.tracker.presentation.ui.theme.GeofencingTheme
import dagger.hilt.android.AndroidEntryPoint
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.geofencing.tracker.utils.BatteryOptimizationHelper
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GeofencingTheme {
                MainScreen()

            }
        }
    }
}

@Composable
fun PermissionHandler(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var permissionsGranted by remember { mutableStateOf(false) }
    var showRationale by remember { mutableStateOf(false) }
    var showBatteryOptimization by remember { mutableStateOf(false) }

    val foregroundPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] == true
        } else {
            true
        }

        if (locationGranted) {
            permissionsGranted = true
        } else {
            showRationale = true
        }
    }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showBatteryOptimization = true
        }
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        foregroundPermissionLauncher.launch(permissions.toTypedArray())
    }

    LaunchedEffect(permissionsGranted) {
        if (permissionsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            delay(1000)
            backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    LaunchedEffect(showBatteryOptimization) {
        if (showBatteryOptimization) {
            delay(2000)
            BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context)
        }
    }

    when {
        permissionsGranted -> {
            content()
        }

        showRationale -> {
            PermissionRationaleScreen(
                onRequestAgain = {
                    showRationale = false
                    foregroundPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                onOpenSettings = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            )
        }

        else -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun PermissionRationaleScreen(
    onRequestAgain: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Location Permission Required",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "This app needs location permission to monitor geofences and show your position on the map.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRequestAgain,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant Permission")
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Settings")
            }
        }
    }
}




