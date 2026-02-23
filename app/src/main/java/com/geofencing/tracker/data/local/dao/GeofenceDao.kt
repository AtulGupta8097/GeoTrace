package com.geofencing.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    @Query("SELECT * FROM geofences ORDER BY createdAt DESC")
    suspend fun getAllGeofencesSnapshot(): List<GeofenceEntity>

    @Query("UPDATE geofences SET isSelected = :isSelected WHERE id = :geofenceId")
    suspend fun updateSelection(geofenceId: Long, isSelected: Boolean)

    @Query("UPDATE geofences SET isSelected = 0")
    suspend fun clearAllSelections()

    @Query("UPDATE geofences SET isVisited = :isVisited WHERE id = :geofenceId")
    suspend fun updateVisited(geofenceId: Long, isVisited: Boolean)

    @Query("UPDATE geofences SET isVisited = 0")
    suspend fun clearAllVisited()

    @Query("SELECT * FROM geofences WHERE isSelected = 1 ORDER BY createdAt DESC")
    suspend fun getSelectedGeofences(): List<GeofenceEntity>
}
