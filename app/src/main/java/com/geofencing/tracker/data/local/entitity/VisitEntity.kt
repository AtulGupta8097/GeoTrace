package com.geofencing.tracker.data.local.entitity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "visits",
    foreignKeys = [
        ForeignKey(
            entity = GeofenceEntity::class,
            parentColumns = ["id"],
            childColumns = ["geofenceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("geofenceId")]
)
data class VisitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val geofenceId: Long,
    val geofenceName: String,
    val entryTime: Long,
    val exitTime: Long? = null,
    val durationMinutes: Int
)