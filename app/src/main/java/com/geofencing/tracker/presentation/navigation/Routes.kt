package com.geofencing.tracker.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Routes : NavKey {

    @Serializable
    data object Map : Routes, NavKey

    @Serializable
    data object Geofence : Routes, NavKey

    @Serializable
    data object Route : Routes, NavKey

    @Serializable
    data object Visit : Routes, NavKey
}
