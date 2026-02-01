package com.geofencing.tracker.domain.usecase

import com.geofencing.tracker.domain.repository.GeofenceRepository
import javax.inject.Inject

class RemoveGeofenceUseCase @Inject constructor(
    private val repository: GeofenceRepository
) {
    suspend operator fun invoke(geofenceId: Long) {
        repository.removeGeofence(geofenceId)
    }
}