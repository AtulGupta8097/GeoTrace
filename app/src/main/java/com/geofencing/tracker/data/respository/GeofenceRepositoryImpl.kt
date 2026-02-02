package com.geofencing.tracker.data.respository
import com.geofencing.tracker.data.local.dao.GeofenceDao
import com.geofencing.tracker.data.local.dao.VisitDao
import com.geofencing.tracker.data.mapper.toDomain
import com.geofencing.tracker.data.mapper.toEntity
import com.geofencing.tracker.domain.model.GeofenceLocation
import com.geofencing.tracker.domain.model.GeofenceVisit
import com.geofencing.tracker.domain.repository.GeofenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GeofenceRepositoryImpl @Inject constructor(
    private val geofenceDao: GeofenceDao,
    private val visitDao: VisitDao
) : GeofenceRepository {

    override suspend fun addGeofence(geofence: GeofenceLocation): Long {
        return geofenceDao.insert(geofence.toEntity())
    }

    override suspend fun removeGeofence(geofenceId: Long) {
        geofenceDao.deleteById(geofenceId)
    }

    override suspend fun getGeofence(geofenceId: Long): GeofenceLocation? {
        return geofenceDao.getGeofenceById(geofenceId)?.toDomain()
    }

    override fun getAllGeofences(): Flow<List<GeofenceLocation>> {
        return geofenceDao.getAllGeofences().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addVisit(visit: GeofenceVisit): Long {
        return visitDao.insert(visit.toEntity())
    }

    override suspend fun updateVisitExitTime(
        visitId: Long,
        exitTime: Long
    ) {
        val visit = visitDao.getVisitById(visitId) ?: return

        val durationMinutes =
            ((exitTime - visit.entryTime) / 60_000).toInt()

        visitDao.updateExitTime(
            visitId = visitId,
            exitTime = exitTime,
            durationMinutes = durationMinutes
        )
    }


    override suspend fun getActiveVisit(geofenceId: Long): GeofenceVisit? {
        return visitDao.getActiveVisit(geofenceId)?.toDomain()
    }

    override fun getAllVisits(): Flow<List<GeofenceVisit>> {
        return visitDao.getAllVisits().map { entities ->
            entities.map { it.toDomain() }
        }
    }
}