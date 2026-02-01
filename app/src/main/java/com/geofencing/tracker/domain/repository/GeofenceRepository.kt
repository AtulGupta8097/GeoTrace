package com.geofencing.tracker.domain.repository

import com.geofencing.tracker.domain.model.GeofenceLocation
import com.geofencing.tracker.domain.model.GeofenceVisit
import kotlinx.coroutines.flow.Flow

interface GeofenceRepository {
    suspend fun addGeofence(geofence: GeofenceLocation): Long
    suspend fun removeGeofence(geofenceId: Long)
    suspend fun getGeofence(geofenceId: Long): GeofenceLocation?
    fun getAllGeofences(): Flow<List<GeofenceLocation>>
    
    suspend fun addVisit(visit: GeofenceVisit): Long
    suspend fun updateVisitExitTime(visitId: Long, exitTime: Long)
    suspend fun getActiveVisit(geofenceId: Long): GeofenceVisit?
    fun getAllVisits(): Flow<List<GeofenceVisit>>
}