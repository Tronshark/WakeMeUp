package com.example.wakemeup.alarm

// Author: TronShark
// Version: v1.0

import android.content.Context
import android.util.Log
import com.example.wakemeup.location.GeofenceManager
import com.example.wakemeup.location.LocationForegroundService
import com.example.wakemeup.model.Destination
import com.example.wakemeup.widget.WakeMeUpWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ActiveAlarmStore {
    private const val TAG = "ActiveAlarmStore"
    private const val PREFS_NAME = "wakemeup_alarm_prefs"

    private const val KEY_IS_ACTIVE = "is_active"
    private const val KEY_DEST_TITLE = "dest_title"
    private const val KEY_DEST_ADDRESS = "dest_address"
    private const val KEY_DEST_LAT = "dest_lat"
    private const val KEY_DEST_LON = "dest_lon"
    private const val KEY_RADIUS_METERS = "radius_meters"
    private const val KEY_HISTORY_ID = "history_id"

    private val _activeDestination = MutableStateFlow<Destination?>(null)
    val activeDestination: StateFlow<Destination?> = _activeDestination.asStateFlow()

    private val _radiusMeters = MutableStateFlow(1000f)
    val radiusMeters: StateFlow<Float> = _radiusMeters.asStateFlow()

    private val _isAlarmTriggered = MutableStateFlow(false)
    val isAlarmTriggered: StateFlow<Boolean> = _isAlarmTriggered.asStateFlow()
    
    var currentHistoryId: String? = null

    fun saveState(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dest = _activeDestination.value
        val isActive = dest != null
        prefs.edit()
            .putBoolean(KEY_IS_ACTIVE, isActive)
            .putString(KEY_DEST_TITLE, dest?.title ?: "")
            .putString(KEY_DEST_ADDRESS, dest?.address ?: "")
            .putString(KEY_DEST_LAT, (dest?.latitude ?: 0.0).toString())
            .putString(KEY_DEST_LON, (dest?.longitude ?: 0.0).toString())
            .putFloat(KEY_RADIUS_METERS, _radiusMeters.value)
            .putString(KEY_HISTORY_ID, currentHistoryId)
            .apply()
        Log.i(TAG, "Saved alarm state: isActive=$isActive, dest=${dest?.title}")
    }

    fun restoreState(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isActive = prefs.getBoolean(KEY_IS_ACTIVE, false)
        if (isActive) {
            val title = prefs.getString(KEY_DEST_TITLE, "") ?: ""
            val address = prefs.getString(KEY_DEST_ADDRESS, "") ?: ""
            val lat = prefs.getString(KEY_DEST_LAT, "0.0")?.toDoubleOrNull() ?: 0.0
            val lon = prefs.getString(KEY_DEST_LON, "0.0")?.toDoubleOrNull() ?: 0.0
            val radius = prefs.getFloat(KEY_RADIUS_METERS, 1000f)
            currentHistoryId = prefs.getString(KEY_HISTORY_ID, null)

            if (title.isNotBlank()) {
                val dest = Destination(
                    title = title,
                    address = address,
                    latitude = lat,
                    longitude = lon
                )
                _activeDestination.value = dest
                _radiusMeters.value = radius
                Log.i(TAG, "Restored active alarm for destination: $title, radius: $radius m")
            }
        }
    }

    fun setActiveAlarm(destination: Destination, radius: Float, context: Context? = null) {
        _activeDestination.value = destination
        _radiusMeters.value = radius
        _isAlarmTriggered.value = false
        Log.i(TAG, "Active alarm set for destination: ${destination.title}, radius: $radius m")
        context?.let {
            currentHistoryId = AlarmHistoryManager.recordAlarmSet(it, destination.title)
            saveState(it)
            AlarmNotificationHelper.postNotification(
                context = it,
                destination = destination,
                location = LocationForegroundService.currentLocation.value
            )
            WakeMeUpWidgetProvider.updateWidget(it)
        }
    }

    fun triggerAlarm(context: Context) {
        if (_isAlarmTriggered.value) return
        _isAlarmTriggered.value = true
        Log.i(TAG, "Triggering alarm alert!")

        AlarmSoundVibratorManager.startAlarm(context)
        
        currentHistoryId?.let {
            AlarmHistoryManager.recordAlarmRing(context, it)
        }

        val dest = _activeDestination.value
        val title = dest?.title ?: "Destination Reached"
        val address = dest?.address ?: "You are within your alarm radius"

        AlarmNotificationHelper.showAlarmNotification(context, title, address)
        AlarmActivity.launch(context, title, address)
    }

    fun stopAlarm(context: Context) {
        Log.i(TAG, "Stopping alarm and clearing geofences")
        _isAlarmTriggered.value = false
        _activeDestination.value = null
        currentHistoryId = null
        saveState(context)
        AlarmSoundVibratorManager.stopAlarm()
        AlarmNotificationHelper.cancelAlarmNotification(context)
        AlarmNotificationHelper.cancelOngoingNotification(context)
        LocationForegroundService.stopService(context)
        GeofenceManager(context).clearAllGeofences()
        WakeMeUpWidgetProvider.updateWidget(context)
    }
}
