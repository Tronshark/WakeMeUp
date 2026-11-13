package com.example.wakemeup.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.wakemeup.MainActivity
import com.example.wakemeup.R
import com.example.wakemeup.alarm.ActiveAlarmStore
import com.example.wakemeup.alarm.AlarmNotificationHelper
import com.example.wakemeup.alarm.AlarmStopReceiver
import com.example.wakemeup.widget.WakeMeUpWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class LocationForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient: LocationClient

    override fun onCreate() {
        super.onCreate()
        locationClient = DefaultLocationClient(applicationContext)
        ActiveAlarmStore.restoreState(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForegroundService()
        } else {
            startForegroundService()
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val activeDest = ActiveAlarmStore.activeDestination.value
        val initialLocation = _currentLocation.value

        val notification = if (activeDest != null) {
            val initialDist = initialLocation?.let {
                calculateDistance(it.latitude, it.longitude, activeDest.latitude, activeDest.longitude)
            }
            AlarmNotificationHelper.postNotification(
                context = applicationContext,
                destination = activeDest,
                location = initialLocation,
                distanceMeters = initialDist
            )
        } else {
            createNotification("WakeMeUp Active", "Monitoring location for wake up alarms...")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        _isServiceRunning.value = true

        locationClient.getLocationUpdates(LOCATION_UPDATE_INTERVAL_MS)
            .catch { e -> Log.e(TAG, "Location update error", e) }
            .onEach { location ->
                _currentLocation.value = location

                val dest = ActiveAlarmStore.activeDestination.value
                val radius = ActiveAlarmStore.radiusMeters.value

                if (dest != null) {
                    val distanceMeters = calculateDistance(
                        location.latitude, location.longitude,
                        dest.latitude, dest.longitude
                    )

                    AlarmNotificationHelper.postNotification(
                        context = applicationContext,
                        destination = dest,
                        location = location,
                        distanceMeters = distanceMeters
                    )

                    if (!ActiveAlarmStore.isAlarmTriggered.value && distanceMeters <= radius) {
                        Log.i(TAG, "User location is within destination radius ($distanceMeters m <= $radius m). Triggering alarm!")
                        ActiveAlarmStore.triggerAlarm(applicationContext)
                    }
                } else {
                    updateDefaultNotification(
                        "WakeMeUp Active",
                        "Lat: %.4f, Lng: %.4f".format(location.latitude, location.longitude)
                    )
                }

                WakeMeUpWidgetProvider.updateWidget(applicationContext)
            }
            .launchIn(serviceScope)
    }

    private fun calculateDistance(startLat: Double, startLng: Double, endLat: Double, endLng: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(startLat, startLng, endLat, endLng, results)
        return results[0]
    }

    private fun stopForegroundService() {
        _isServiceRunning.value = false
        _currentLocation.value = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        AlarmNotificationHelper.cancelOngoingNotification(applicationContext)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Monitoring Service",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notification for active location monitoring and geofencing"
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = Intent(this, AlarmStopReceiver::class.java).apply {
            action = AlarmStopReceiver.ACTION_STOP_ALARM
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this,
            REQUEST_CODE_STOP_SERVICE_NOTIF,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(
                R.mipmap.ic_launcher,
                "Stop Alarm",
                stopPendingIntent
            )
            .build()
    }

    private fun updateDefaultNotification(title: String, contentText: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(title, contentText))
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        _isServiceRunning.value = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "LocationService"
        private const val REQUEST_CODE_STOP_SERVICE_NOTIF = 1002
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val CHANNEL_ID = "wakemeup_location_channel"
        const val NOTIFICATION_ID = 1001
        const val LOCATION_UPDATE_INTERVAL_MS = 10000L

        private val _currentLocation = MutableStateFlow<Location?>(null)
        val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

        private val _isServiceRunning = MutableStateFlow(value = false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, LocationForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, LocationForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
