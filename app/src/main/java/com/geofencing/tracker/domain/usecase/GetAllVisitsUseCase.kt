package com.geofencing.tracker.domain.usecase

import com.geofencing.tracker.domain.model.GeofenceVisit
import com.geofencing.tracker.domain.repository.GeofenceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllVisitsUseCase @Inject constructor(
    private val repository: GeofenceRepository
) {
    operator fun invoke(): Flow<List<GeofenceVisit>> {
        return repository.getAllVisits()
    }
}