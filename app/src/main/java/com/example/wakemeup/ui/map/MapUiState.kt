package com.example.wakemeup.ui.map

import com.example.wakemeup.model.Destination
import org.osmdroid.util.GeoPoint

data class MapUiState(
    val userLocation: GeoPoint? = null,
    val selectedDestination: Destination? = null,
    val radiusMeters: Float = 1000f, // Range: 500m to 5000m (5km)
    val searchQuery: String = "",
    val searchResults: List<Destination> = emptyList(),
    val isSearching: Boolean = false,
    val isAlarmActive: Boolean = false,
    val distanceToDestinationMeters: Float? = null,
    val errorMessage: String? = null,
    val hasLocationPermission: Boolean = false,
    val cameraTargetPosition: GeoPoint? = null
) {
    val isInsideRadius: Boolean
        get() {
            val dist = distanceToDestinationMeters ?: return false
            return dist <= radiusMeters
        }
}
