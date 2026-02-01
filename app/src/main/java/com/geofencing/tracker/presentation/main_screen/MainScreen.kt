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
import com.geofencing.tracker.presentation.main_screen.component.BottomBar
import com.geofencing.tracker.presentation.navigation.Routes

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
                    entry<Routes.Geofence> {
                        GeofenceScreen()
                    }
                    entry<Routes.Visit> {
                        VisitScreen()
                    }
                    entry<Routes.History> {

                    }
                }

            )
        }
    }
}

@Composable
fun GeofenceScreen() {

}
@Composable
fun VisitScreen() {

}

