package com.geofencing.tracker.domain.usecase
import com.geofencing.tracker.domain.repository.GeofenceRepository
import javax.inject.Inject

class RecordGeofenceExitUseCase @Inject constructor(
    private val repository: GeofenceRepository
) {
    suspend operator fun invoke(geofenceId: Long) {
        val activeVisit = repository.getActiveVisit(geofenceId)
        activeVisit?.let {
            repository.updateVisitExitTime(it.id, System.currentTimeMillis())
        }
    }
}