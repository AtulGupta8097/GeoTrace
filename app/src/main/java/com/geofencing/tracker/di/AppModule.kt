package com.geofencing.tracker.di

import android.content.Context
import androidx.room.Room
import com.geofencing.tracker.data.local.dao.GeofenceDao
import com.geofencing.tracker.data.local.dao.VisitDao
import com.geofencing.tracker.data.local.database.GeofenceDatabase
import com.geofencing.tracker.data.respository.GeofenceRepositoryImpl
import com.geofencing.tracker.domain.repository.GeofenceRepository
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
    fun provideGeofenceDatabase(@ApplicationContext context: Context): GeofenceDatabase {
        return Room.databaseBuilder(
            context,
            GeofenceDatabase::class.java,
            "geofence_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideGeofenceDao(database: GeofenceDatabase): GeofenceDao {
        return database.geofenceDao()
    }

    @Provides
    @Singleton
    fun provideVisitDao(database: GeofenceDatabase): VisitDao {
        return database.visitDao()
    }

    @Provides
    @Singleton
    fun provideGeofenceRepository(
        geofenceDao: GeofenceDao,
        visitDao: VisitDao
    ): GeofenceRepository {
        return GeofenceRepositoryImpl(geofenceDao, visitDao)
    }
}