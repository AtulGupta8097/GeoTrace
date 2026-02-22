package com.geofencing.tracker.domain.model

data class GeofenceLocation(
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Float,
    val createdAt: Long = System.currentTimeMillis(),
    val isSelected: Boolean = false,
    val isVisited: Boolean = false
)
