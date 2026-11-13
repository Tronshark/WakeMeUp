package com.example.wakemeup.ui.map

// Author: TronShark
// Version: v1.0

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wakemeup.alarm.ActiveAlarmStore
import com.example.wakemeup.alarm.AlarmNotificationHelper
import com.example.wakemeup.data.DefaultLocationSearchRepository
import com.example.wakemeup.data.LocationSearchRepository
import com.example.wakemeup.location.GeofenceManager
import com.example.wakemeup.location.LocationForegroundService
import com.example.wakemeup.model.Destination
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.osmdroid.util.GeoPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MapViewModel(
    private val locationSearchRepository: LocationSearchRepository,
    private val geofenceManager: GeofenceManager? = null,
    private val fusedLocationProviderClient: FusedLocationProviderClient? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        observeServiceState()
        observeUserLocation()
        observeActiveAlarmStore()
        fetchCurrentLocation()
    }

    private fun observeActiveAlarmStore() {
        viewModelScope.launch {
            ActiveAlarmStore.activeDestination.collect { activeDest ->
                if (activeDest != null) {
                    _uiState.update { currentState ->
                        currentState.copy(
                            selectedDestination = activeDest,
                            radiusMeters = ActiveAlarmStore.radiusMeters.value,
                            searchQuery = activeDest.title,
                            cameraTargetPosition = activeDest.geoPoint
                        )
                    }
                }
            }
        }
    }

    private fun observeServiceState() {
        viewModelScope.launch {
            LocationForegroundService.isServiceRunning.collect { isRunning ->
                _uiState.update { it.copy(isAlarmActive = isRunning) }
            }
        }
    }

    private fun observeUserLocation() {
        viewModelScope.launch {
            LocationForegroundService.currentLocation.collect { location ->
                if (location != null) {
                    updateUserLocation(location)
                }
            }
        }
    }

    fun onPermissionStatusChanged(hasPermission: Boolean) {
        _uiState.update { it.copy(hasLocationPermission = hasPermission) }
        if (hasPermission) {
            fetchCurrentLocation()
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation() {
        if (fusedLocationProviderClient == null) return
        try {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    updateUserLocation(location)
                } else {
                    fusedLocationProviderClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        null
                    ).addOnSuccessListener { currentLocation: Location? ->
                        if (currentLocation != null) {
                            updateUserLocation(currentLocation)
                        }
                    }
                }
            }
        } catch (_: SecurityException) {
            // Missing location permission
        }
    }

    @SuppressLint("MissingPermission")
    fun centerOnUserLocation() {
        if (fusedLocationProviderClient != null) {
            try {
                fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val userGeoPoint = GeoPoint(location.latitude, location.longitude)
                        _uiState.update { currentState ->
                            val distance = currentState.selectedDestination?.let { dest ->
                                calculateDistance(userGeoPoint, dest.geoPoint)
                            }
                            currentState.copy(
                                userLocation = userGeoPoint,
                                distanceToDestinationMeters = distance,
                                cameraTargetPosition = userGeoPoint
                            )
                        }
                    } else {
                        fusedLocationProviderClient.getCurrentLocation(
                            Priority.PRIORITY_HIGH_ACCURACY,
                            null
                        ).addOnSuccessListener { currentLocation: Location? ->
                            if (currentLocation != null) {
                                val userGeoPoint = GeoPoint(currentLocation.latitude, currentLocation.longitude)
                                _uiState.update { currentState ->
                                    val distance = currentState.selectedDestination?.let { dest ->
                                        calculateDistance(userGeoPoint, dest.geoPoint)
                                    }
                                    currentState.copy(
                                        userLocation = userGeoPoint,
                                        distanceToDestinationMeters = distance,
                                        cameraTargetPosition = userGeoPoint
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (_: SecurityException) {
                _uiState.value.userLocation?.let { userPos ->
                    _uiState.update { it.copy(cameraTargetPosition = userPos) }
                }
            }
        } else {
            _uiState.value.userLocation?.let { userPos ->
                _uiState.update { it.copy(cameraTargetPosition = userPos) }
            }
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(400) // Debounce search
            performSearch(query)
        }
    }

    fun performSearch(
        query: String = _uiState.value.searchQuery,
        mapCenter: GeoPoint? = _uiState.value.userLocation
    ) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, errorMessage = null) }
            val center = mapCenter ?: _uiState.value.userLocation
            val results = locationSearchRepository.searchLocations(query, mapCenter = center)
            _uiState.update {
                it.copy(
                    searchResults = results,
                    isSearching = false,
                    errorMessage = if (results.isEmpty()) "No locations found for '$query'" else null
                )
            }
        }
    }

    fun selectDestination(destination: Destination) {
        _uiState.update { currentState ->
            val distance = currentState.userLocation?.let { user ->
                calculateDistance(user, destination.geoPoint)
            }
            currentState.copy(
                selectedDestination = destination,
                searchQuery = destination.title,
                searchResults = emptyList(),
                distanceToDestinationMeters = distance,
                cameraTargetPosition = destination.geoPoint,
                errorMessage = null
            )
        }
    }

    fun selectLocation(geoPoint: GeoPoint) {
        viewModelScope.launch {
            val destination = locationSearchRepository.getAddressFromLocation(
                geoPoint.latitude,
                geoPoint.longitude
            )
            selectDestination(destination)
        }
    }

    fun onMapClick(geoPoint: GeoPoint) {
        selectLocation(geoPoint)
    }

    fun onMarkerDragEnd(geoPoint: GeoPoint) {
        selectLocation(geoPoint)
    }

    fun onRadiusChanged(radiusMeters: Float) {
        val clampedRadius = radiusMeters.coerceIn(500f, 5000f)
        _uiState.update { currentState ->
            currentState.copy(radiusMeters = clampedRadius)
        }

        // If alarm is active, update active alarm store & geofence with new radius
        val dest = _uiState.value.selectedDestination
        if (_uiState.value.isAlarmActive && dest != null) {
            ActiveAlarmStore.setActiveAlarm(dest, clampedRadius)
            if (geofenceManager != null) {
                geofenceManager.addGeofence(
                    key = GEOFENCE_KEY,
                    latitude = dest.latitude,
                    longitude = dest.longitude,
                    radiusInMeters = clampedRadius
                )
            }
        }
    }

    fun toggleAlarm(context: Context) {
        val currentState = _uiState.value
        val destination = currentState.selectedDestination

        if (!currentState.isAlarmActive) {
            if (destination == null) {
                _uiState.update { it.copy(errorMessage = "Please select a destination first") }
                return
            }

            val initialDistance = currentState.userLocation?.let { user ->
                calculateDistance(user, destination.geoPoint)
            }
            val currentLocation = LocationForegroundService.currentLocation.value

            ActiveAlarmStore.setActiveAlarm(destination, currentState.radiusMeters, context)
            AlarmNotificationHelper.postNotification(
                context = context,
                destination = destination,
                location = currentLocation,
                distanceMeters = initialDistance
            )

            val manager = geofenceManager ?: GeofenceManager(context)
            manager.addGeofence(
                key = GEOFENCE_KEY,
                latitude = destination.latitude,
                longitude = destination.longitude,
                radiusInMeters = currentState.radiusMeters,
                onSuccess = {
                    LocationForegroundService.startService(context)
                    _uiState.update { it.copy(isAlarmActive = true, errorMessage = null) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(errorMessage = "Failed to set geofence: ${e.localizedMessage}") }
                }
            )
        } else {
            ActiveAlarmStore.stopAlarm(context)
            _uiState.update { it.copy(isAlarmActive = false) }
        }
    }

    fun clearDestination() {
        _uiState.update {
            it.copy(
                selectedDestination = null,
                searchQuery = "",
                searchResults = emptyList(),
                distanceToDestinationMeters = null,
                cameraTargetPosition = null
            )
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun centerCameraOnUserLocation() {
        centerOnUserLocation()
    }

    fun onCameraMoved() {
        _uiState.update { it.copy(cameraTargetPosition = null) }
    }

    private fun updateUserLocation(location: Location) {
        val userGeoPoint = GeoPoint(location.latitude, location.longitude)
        _uiState.update { currentState ->
            val distance = currentState.selectedDestination?.let { dest ->
                calculateDistance(userGeoPoint, dest.geoPoint)
            }
            currentState.copy(
                userLocation = userGeoPoint,
                distanceToDestinationMeters = distance
            )
        }
    }

    private fun calculateDistance(start: GeoPoint, end: GeoPoint): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            start.latitude, start.longitude,
            end.latitude, end.longitude,
            results
        )
        return results[0]
    }

    companion object {
        const val GEOFENCE_KEY = "WAKE_ME_UP_DESTINATION_GEOFENCE"

        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repository = DefaultLocationSearchRepository(context.applicationContext)
                val manager = GeofenceManager(context.applicationContext)
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context.applicationContext)
                return MapViewModel(repository, manager, fusedLocationClient) as T
            }
        }
    }
}
