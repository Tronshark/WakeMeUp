package com.example.wakemeup.model

import org.osmdroid.util.GeoPoint
import java.util.UUID
import kotlin.math.roundToInt

data class Destination(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val category: String? = null,
    val type: String? = null,
    val distanceMeters: Float? = null
) {
    val geoPoint: GeoPoint
        get() = GeoPoint(latitude, longitude)

    val formattedDistance: String?
        get() = distanceMeters?.let { formatDistance(it) }

    companion object {
        fun formatDistance(meters: Float): String {
            return if (meters < 1000f) {
                "${meters.roundToInt()} m away"
            } else {
                val km = meters / 1000f
                if (km >= 100f) {
                    "${km.roundToInt()} km away"
                } else {
                    String.format(java.util.Locale.US, "%.1f km away", km)
                }
            }
        }
    }
}
