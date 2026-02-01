package com.geofencing.tracker.domain.model

data class GeofenceVisit(
    val id: Long = 0,
    val geofenceId: Long,
    val geofenceName: String,
    val entryTime: Long,
    val exitTime: Long? = null,
    val durationMinutes: Int = 0
)