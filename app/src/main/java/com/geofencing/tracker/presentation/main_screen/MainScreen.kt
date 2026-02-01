package com.geofencing.tracker.presentation.main_screen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.geofencing.tracker.presentation.geofences.GeofencesScreen
import com.geofencing.tracker.presentation.main_screen.component.BottomBar
import com.geofencing.tracker.presentation.map.MapScreen
import com.geofencing.tracker.presentation.navigation.Routes
import com.geofencing.tracker.presentation.visit.VisitsScreen

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun MainScreen() {

    val navBackStack = rememberNavBackStack(Routes.Geofence)

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
        Box(Modifier.padding(padding)) {
            NavDisplay(
                backStack = navBackStack,
                entryProvider = entryProvider {
                    entry<Routes.Map> {
                        MapScreen()
                    }
                    entry<Routes.Geofence> {
                        GeofencesScreen()
                    }
                    entry<Routes.Visit> {
                        VisitsScreen()
                    }
                }

            )
        }
    }
}

