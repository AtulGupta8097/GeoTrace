package com.geofencing.tracker.presentation.main_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.geofencing.tracker.presentation.geofence.GeofencesScreen
import com.geofencing.tracker.presentation.main_screen.component.BottomBar
import com.geofencing.tracker.presentation.map.MapScreen
import com.geofencing.tracker.presentation.navigation.Routes
import com.geofencing.tracker.presentation.route.RouteScreen
import com.geofencing.tracker.presentation.visit.VisitsScreen

@Composable
fun MainScreen() {
    val navBackStack = rememberNavBackStack(Routes.Map)

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
                    entry<Routes.Geofence> { GeofencesScreen() }
                    entry<Routes.Route> { RouteScreen() }
                    entry<Routes.Visit> { VisitsScreen() }
                }
            )
        }
    }
}
