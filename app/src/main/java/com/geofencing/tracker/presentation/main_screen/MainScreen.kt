package com.geofencing.tracker.presentation.main_screen

import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.geofencing.tracker.data.service.GeofenceService
import com.geofencing.tracker.presentation.geofences.GeofencesScreen
import com.geofencing.tracker.presentation.main_screen.component.BottomBar
import com.geofencing.tracker.presentation.map.MapScreen
import com.geofencing.tracker.presentation.navigation.Routes
import com.geofencing.tracker.presentation.route.RouteScreen
import com.geofencing.tracker.presentation.visit.VisitsScreen

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val navBackStack = rememberNavBackStack(Routes.Map)

    // Start the persistent foreground service as soon as the app launches
    LaunchedEffect(Unit) {
        val serviceIntent = Intent(context, GeofenceService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    Scaffold(
        bottomBar = {
            BottomBar(
                current = navBackStack.last(),
                onNavigate = { route ->
                    navBackStack.clear()
                    navBackStack.add(route)
                }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            NavDisplay(
                backStack = navBackStack,
                entryProvider = entryProvider {
                    entry<Routes.Map> {
                        MapScreen(
                            onNavigateToRoute = {
                                navBackStack.clear()
                                navBackStack.add(Routes.Route)
                            }
                        )
                    }
                    entry<Routes.Geofence> {
                        GeofencesScreen()
                    }
                    entry<Routes.Route> {
                        RouteScreen()
                    }
                    entry<Routes.Visit> {
                        VisitsScreen()
                    }
                }
            )
        }
    }
}
