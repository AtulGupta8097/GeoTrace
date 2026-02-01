package com.geofencing.tracker.domain.usecase

import com.geofencing.tracker.domain.model.GeofenceLocation
import com.geofencing.tracker.domain.repository.GeofenceRepository
import javax.inject.Inject

class AddGeofenceUseCase @Inject constructor(
    private val repository: GeofenceRepository
) {
    suspend operator fun invoke(geofence: GeofenceLocation): Long {
        return repository.addGeofence(geofence)
    }
}