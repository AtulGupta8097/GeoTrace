package com.geofencing.tracker.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.geofencing.tracker.data.local.dao.GeofenceDao
import com.geofencing.tracker.data.local.dao.VisitDao
import com.geofencing.tracker.data.local.entitity.GeofenceEntity
import com.geofencing.tracker.data.local.entitity.VisitEntity

@Database(
    entities = [GeofenceEntity::class, VisitEntity::class],
    version = 1,
    exportSchema = false
)
abstract class GeofenceDatabase : RoomDatabase() {
    abstract fun geofenceDao(): GeofenceDao
    abstract fun visitDao(): VisitDao
}