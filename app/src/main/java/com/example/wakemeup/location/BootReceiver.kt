package com.example.wakemeup.location

// Author: TronShark
// Version: v1.0

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.wakemeup.alarm.ActiveAlarmStore
import com.example.wakemeup.ui.map.MapViewModel
import com.example.wakemeup.widget.WakeMeUpWidgetProvider

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i(TAG, "Boot completed event received. Restoring active alarm state...")
            ActiveAlarmStore.restoreState(context)
            val activeDest = ActiveAlarmStore.activeDestination.value
            val radius = ActiveAlarmStore.radiusMeters.value

            if (activeDest != null) {
                Log.i(TAG, "Active alarm found on boot: ${activeDest.title}. Resuming location monitoring...")
                GeofenceManager(context).addGeofence(
                    key = MapViewModel.GEOFENCE_KEY,
                    latitude = activeDest.latitude,
                    longitude = activeDest.longitude,
                    radiusInMeters = radius
                )
                LocationForegroundService.startService(context)
                WakeMeUpWidgetProvider.updateWidget(context)
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
