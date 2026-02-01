package com.geofencing.tracker.domain.usecase

import com.geofencing.tracker.domain.model.GeofenceVisit
import com.geofencing.tracker.domain.repository.GeofenceRepository
import javax.inject.Inject

class RecordGeofenceEntryUseCase @Inject constructor(
    private val repository: GeofenceRepository
) {
    suspend operator fun invoke(geofenceId: Long, geofenceName: String): Long {
        val visit = GeofenceVisit(
            geofenceId = geofenceId,
            geofenceName = geofenceName,
            entryTime = System.currentTimeMillis()
        )
        return repository.addVisit(visit)
    }
}