package com.geofencing.tracker.data.repository

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

    override suspend fun addGeofence(geofence: GeofenceLocation): Long =
        geofenceDao.insert(geofence.toEntity())

    override suspend fun removeGeofence(geofenceId: Long) =
        geofenceDao.deleteById(geofenceId)

    override suspend fun getGeofence(geofenceId: Long): GeofenceLocation? =
        geofenceDao.getGeofenceById(geofenceId)?.toDomain()

    override fun getAllGeofences(): Flow<List<GeofenceLocation>> =
        geofenceDao.getAllGeofences().map { it.map { e -> e.toDomain() } }

    override suspend fun getAllGeofencesSnapshot(): List<GeofenceLocation> =
        geofenceDao.getAllGeofencesSnapshot().map { it.toDomain() }

    override suspend fun addVisit(visit: GeofenceVisit): Long =
        visitDao.insert(visit.toEntity())

    override suspend fun updateVisitExitTime(visitId: Long, exitTime: Long) {
        val visit = visitDao.getVisitById(visitId) ?: return
        val durationMinutes = ((exitTime - visit.entryTime) / 60_000).toInt()
        visitDao.updateExitTime(visitId, exitTime, durationMinutes)
    }

    override suspend fun getActiveVisit(geofenceId: Long): GeofenceVisit? =
        visitDao.getActiveVisit(geofenceId)?.toDomain()

    override fun getAllVisits(): Flow<List<GeofenceVisit>> =
        visitDao.getAllVisits().map { it.map { e -> e.toDomain() } }

    override suspend fun toggleGeofenceSelection(geofenceId: Long, isSelected: Boolean) =
        geofenceDao.updateSelection(geofenceId, isSelected)

    override suspend fun clearAllSelections() =
        geofenceDao.clearAllSelections()

    override suspend fun getSelectedGeofences(): List<GeofenceLocation> =
        geofenceDao.getSelectedGeofences().map { it.toDomain() }

    override suspend fun markGeofenceVisited(geofenceId: Long, isVisited: Boolean) =
        geofenceDao.updateVisited(geofenceId, isVisited)

    override suspend fun clearAllVisited() =
        geofenceDao.clearAllVisited()
}
