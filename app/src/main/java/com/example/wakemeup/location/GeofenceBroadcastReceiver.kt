package com.example.wakemeup.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.wakemeup.alarm.ActiveAlarmStore
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_GEOFENCE_EVENT) return

        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            val errorCode = geofencingEvent.errorCode
            Log.e(TAG, "Geofencing error code: $errorCode")
            GeofenceManager.emitEvent(
                GeofenceEvent.Error(errorCode, "Geofence error: $errorCode")
            )
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT
        ) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences ?: emptyList()
            val location = geofencingEvent.triggeringLocation

            for (geofence in triggeringGeofences) {
                val geofenceId = geofence.requestId
                Log.i(TAG, "Geofence transition: $geofenceTransition for $geofenceId")
                GeofenceManager.emitEvent(
                    GeofenceEvent.Transition(
                        geofenceId = geofenceId,
                        transitionType = geofenceTransition,
                        latitude = location?.latitude ?: 0.0,
                        longitude = location?.longitude ?: 0.0
                    )
                )

                if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                    Log.i(TAG, "Geofence ENTER event for $geofenceId. Triggering wake up alarm!")
                    ActiveAlarmStore.triggerAlarm(context)
                }
            }
        }
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
        const val ACTION_GEOFENCE_EVENT = "com.example.wakemeup.ACTION_GEOFENCE_EVENT"
    }
}
