package com.geofencing.tracker.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.InsertChart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Navbar(
    val route: Routes,
    val title: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
) {
    object Geofence : Navbar(
        route = Routes.Geofence,
        title = "Geofence",
        activeIcon = Icons.Filled.Home,
        inactiveIcon = Icons.Outlined.Home
    )

    object Visits : Navbar(
        route = Routes.Visit,
        title = "Visits",
        activeIcon = Icons.Filled.InsertChart,
        inactiveIcon = Icons.Outlined.InsertChart
    )

    object History : Navbar(
        route = Routes.History,
        title = "History",
        activeIcon = Icons.Filled.Bookmark,
        inactiveIcon = Icons.Outlined.Bookmark
    )

    companion object {
        val items = listOf(Geofence, Visits, History)
    }
}
