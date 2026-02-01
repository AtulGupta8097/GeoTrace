// data/local/dao/GeofenceDao.kt
package com.geofencing.tracker.data.local.dao

import androidx.room.*
import com.geofencing.tracker.data.local.entitity.GeofenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GeofenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(geofence: GeofenceEntity): Long

    @Delete
    suspend fun delete(geofence: GeofenceEntity)

    @Query("DELETE FROM geofences WHERE id = :geofenceId")
    suspend fun deleteById(geofenceId: Long)

    @Query("SELECT * FROM geofences WHERE id = :geofenceId")
    suspend fun getGeofenceById(geofenceId: Long): GeofenceEntity?

    @Query("SELECT * FROM geofences ORDER BY createdAt DESC")
    fun getAllGeofences(): Flow<List<GeofenceEntity>>
}