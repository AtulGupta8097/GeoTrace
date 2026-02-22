package com.geofencing.tracker.di

import android.content.Context
import androidx.room.Room
import com.geofencing.tracker.data.local.dao.GeofenceDao
import com.geofencing.tracker.data.local.dao.VisitDao
import com.geofencing.tracker.data.local.database.GeofenceDatabase
import com.geofencing.tracker.data.repository.GeofenceRepositoryImpl  // ← fixed typo
import com.geofencing.tracker.domain.repository.GeofenceRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGeofenceDatabase(@ApplicationContext context: Context): GeofenceDatabase =
        Room.databaseBuilder(context, GeofenceDatabase::class.java, "geofence_database")
            .addMigrations(GeofenceDatabase.MIGRATION_1_2)
            .build()

    @Provides
    @Singleton
    fun provideGeofenceDao(database: GeofenceDatabase): GeofenceDao =
        database.geofenceDao()

    @Provides
    @Singleton
    fun provideVisitDao(database: GeofenceDatabase): VisitDao =
        database.visitDao()

    @Provides
    @Singleton
    fun provideGeofenceRepository(
        geofenceDao: GeofenceDao,
        visitDao: VisitDao
    ): GeofenceRepository = GeofenceRepositoryImpl(geofenceDao, visitDao)

    /**
     * Provides FusedLocationProviderClient for injection into GeofenceManager
     * and MapViewModel — replaces the old pattern of creating it inside each class.
     */
    @Provides
    @Singleton
    fun provideFusedLocationClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
}