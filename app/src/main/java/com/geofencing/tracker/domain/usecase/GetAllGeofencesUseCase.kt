package com.geofencing.tracker.domain.usecase

import com.geofencing.tracker.domain.model.GeofenceLocation
import com.geofencing.tracker.domain.repository.GeofenceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllGeofencesUseCase @Inject constructor(
    private val repository: GeofenceRepository
) {
    operator fun invoke(): Flow<List<GeofenceLocation>> {
        return repository.getAllGeofences()
    }
}