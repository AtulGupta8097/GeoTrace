package com.geofencing.tracker.data.mapper

import com.geofencing.tracker.data.local.entitity.GeofenceEntity
import com.geofencing.tracker.data.local.entitity.VisitEntity
import com.geofencing.tracker.domain.model.GeofenceLocation
import com.geofencing.tracker.domain.model.GeofenceVisit

fun GeofenceEntity.toDomain(): GeofenceLocation {
    return GeofenceLocation(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        radius = radius,
        createdAt = createdAt
    )
}

fun GeofenceLocation.toEntity(): GeofenceEntity {
    return GeofenceEntity(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        radius = radius,
        createdAt = createdAt
    )
}

fun VisitEntity.toDomain(): GeofenceVisit {
    return GeofenceVisit(
        id = id,
        geofenceId = geofenceId,
        geofenceName = geofenceName,
        entryTime = entryTime,
        exitTime = exitTime,
        durationMinutes = durationMinutes
    )
}

fun GeofenceVisit.toEntity(): VisitEntity {
    return VisitEntity(
        id = id,
        geofenceId = geofenceId,
        geofenceName = geofenceName,
        entryTime = entryTime,
        exitTime = exitTime,
        durationMinutes = durationMinutes
    )
}