package com.geofencing.tracker.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.ui.graphics.vector.ImageVector

data class NavItem(
    val title: String,
    val route: Routes,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
)

object Navbar {
    val items = listOf(
        NavItem("Map", Routes.Map, Icons.Filled.Map, Icons.Outlined.Map),
        NavItem("Geofences", Routes.Geofence, Icons.Filled.LocationOn, Icons.Outlined.LocationOn),
        NavItem("Route", Routes.Route, Icons.Filled.Navigation, Icons.Outlined.Navigation),
        NavItem("Visits", Routes.Visit, Icons.Filled.Schedule, Icons.Outlined.Schedule),
    )
}
