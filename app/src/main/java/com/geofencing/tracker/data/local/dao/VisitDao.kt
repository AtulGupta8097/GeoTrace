package com.geofencing.tracker.data.local.dao

import androidx.room.*
import com.geofencing.tracker.data.local.entitity.VisitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(visit: VisitEntity): Long

    @Update
    suspend fun update(visit: VisitEntity)
    @Query("SELECT * FROM visits WHERE id = :visitId LIMIT 1")
    suspend fun getVisitById(visitId: Long): VisitEntity

    @Query("SELECT * FROM visits WHERE geofenceId = :geofenceId AND exitTime IS NULL LIMIT 1")
    suspend fun getActiveVisit(geofenceId: Long): VisitEntity?

    @Query("UPDATE visits SET exitTime = :exitTime, durationMinutes = :durationMinutes WHERE id = :visitId")
    suspend fun updateExitTime(visitId: Long, exitTime: Long, durationMinutes: Int)

    @Query("SELECT * FROM visits ORDER BY entryTime DESC")
    fun getAllVisits(): Flow<List<VisitEntity>>
}