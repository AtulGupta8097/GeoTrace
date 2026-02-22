package com.geofencing.tracker.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geofencing.tracker.domain.usecase.GetAllGeofencesUseCase
import com.geofencing.tracker.domain.usecase.RemoveGeofenceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GeofencesViewModel @Inject constructor(
    getAllGeofencesUseCase: GetAllGeofencesUseCase,
    private val removeGeofenceUseCase: RemoveGeofenceUseCase,
) : ViewModel() {

    val geofences = getAllGeofencesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun removeGeofence(geofenceId: Long) {
        viewModelScope.launch {
            // Deletes from Room — GeofenceManager reads from DB on each location tick
            // so it will automatically stop monitoring this geofence
            removeGeofenceUseCase(geofenceId)
        }
    }
}