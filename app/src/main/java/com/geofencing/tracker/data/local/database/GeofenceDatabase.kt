package com.geofencing.tracker.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.geofencing.tracker.data.local.dao.GeofenceDao
import com.geofencing.tracker.data.local.dao.VisitDao
import com.geofencing.tracker.data.local.entitity.GeofenceEntity
import com.geofencing.tracker.data.local.entitity.VisitEntity

@Database(
    entities = [GeofenceEntity::class, VisitEntity::class],
    version = 2,
    exportSchema = false
)
abstract class GeofenceDatabase : RoomDatabase() {
    abstract fun geofenceDao(): GeofenceDao
    abstract fun visitDao(): VisitDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE geofences ADD COLUMN isSelected INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE geofences ADD COLUMN isVisited INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}