package com.example.wakemeup.location

// Author: TronShark
// Version: v1.0

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class GeofenceEvent {
    data class Transition(
        val geofenceId: String,
        val transitionType: Int,
        val latitude: Double,
        val longitude: Double
    ) : GeofenceEvent()

    data class Error(val errorCode: Int, val message: String) : GeofenceEvent()
}

class GeofenceManager(private val context: Context) {

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
            action = GeofenceBroadcastReceiver.ACTION_GEOFENCE_EVENT
        }
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    @SuppressLint("MissingPermission")
    fun addGeofence(
        key: String,
        latitude: Double,
        longitude: Double,
        radiusInMeters: Float,
        expirationDurationMillis: Long = Geofence.NEVER_EXPIRE,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        if (!context.hasLocationPermission()) {
            onFailure(SecurityException("Missing location permission"))
            return
        }

        val geofence = Geofence.Builder()
            .setRequestId(key)
            .setCircularRegion(latitude, longitude, radiusInMeters)
            .setExpirationDuration(expirationDurationMillis)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
            .addOnSuccessListener {
                Log.d(TAG, "Geofence added successfully: $key")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to add geofence: $key", exception)
                onFailure(exception)
            }
    }

    fun removeGeofence(
        key: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        geofencingClient.removeGeofences(listOf(key))
            .addOnSuccessListener {
                Log.d(TAG, "Geofence removed successfully: $key")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to remove geofence: $key", exception)
                onFailure(exception)
            }
    }

    fun clearAllGeofences(
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        geofencingClient.removeGeofences(geofencePendingIntent)
            .addOnSuccessListener {
                Log.d(TAG, "All geofences cleared")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to clear all geofences", exception)
                onFailure(exception)
            }
    }

    companion object {
        private const val TAG = "GeofenceManager"
        private val _geofenceEvents = MutableSharedFlow<GeofenceEvent>(extraBufferCapacity = 64)
        val geofenceEvents: SharedFlow<GeofenceEvent> = _geofenceEvents.asSharedFlow()

        internal fun emitEvent(event: GeofenceEvent) {
            _geofenceEvents.tryEmit(event)
        }
    }
}
