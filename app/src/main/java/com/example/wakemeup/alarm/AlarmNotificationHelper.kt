package com.example.wakemeup.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.wakemeup.MainActivity
import com.example.wakemeup.R
import com.example.wakemeup.model.Destination

object AlarmNotificationHelper {
    private const val HIGH_PRIORITY_CHANNEL_ID = "wakemeup_alarm_high_priority_channel"
    private const val HIGH_PRIORITY_NOTIFICATION_ID = 2002
    private const val REQUEST_CODE_FULL_SCREEN = 3001
    private const val REQUEST_CODE_STOP = 3002

    const val ONGOING_CHANNEL_ID = "wakemeup_location_channel"
    const val ONGOING_NOTIFICATION_ID = 1001
    private const val REQUEST_CODE_STOP_SERVICE_NOTIF = 1002

    fun calculateSpeedKmH(location: Location): Int {
        return (location.speed * 3.6).toInt()
    }

    fun calculateEtaMins(location: Location, distanceMeters: Float): Int {
        val effectiveSpeed = maxOf(location.speed, 5.0f)
        return (distanceMeters / effectiveSpeed / 60).toInt()
    }

    fun formatDistance(distanceMeters: Float): String {
        return if (distanceMeters >= 1000f) {
            "%.1f km".format(distanceMeters / 1000f)
        } else {
            "${distanceMeters.toInt()} m"
        }
    }

    fun formatEta(etaMins: Int): String {
        return if (etaMins <= 0) "< 1 min" else "$etaMins min${if (etaMins > 1) "s" else ""}"
    }

    fun formatContentText(location: Location, distanceMeters: Float): String {
        val formattedDistance = formatDistance(distanceMeters)
        val speedKmH = calculateSpeedKmH(location)
        val etaMins = calculateEtaMins(location, distanceMeters)
        val formattedEta = formatEta(etaMins)
        return "$formattedDistance remaining • $speedKmH km/h • ETA: $formattedEta"
    }

    fun createOngoingNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ONGOING_CHANNEL_ID,
                "Location Monitoring Service",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notification for active location monitoring and geofencing"
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun postNotification(
        context: Context,
        destination: Destination,
        location: Location? = null,
        distanceMeters: Float? = null
    ): Notification {
        createOngoingNotificationChannel(context)

        val contentText = if (location != null && distanceMeters != null) {
            formatContentText(location, distanceMeters)
        } else if (distanceMeters != null) {
            "${formatDistance(distanceMeters)} remaining • 0 km/h • ETA: Calculating..."
        } else {
            "Monitoring location for wake up alarm..."
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, AlarmStopReceiver::class.java).apply {
            action = AlarmStopReceiver.ACTION_STOP_ALARM
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_STOP_SERVICE_NOTIF,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ONGOING_CHANNEL_ID)
            .setContentTitle("WakeMeUp: ${destination.title}")
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

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(ONGOING_NOTIFICATION_ID, notification)
        return notification
    }

    fun cancelOngoingNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(ONGOING_NOTIFICATION_ID)
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                HIGH_PRIORITY_CHANNEL_ID,
                "Destination Wake-Up Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority notifications with full-screen intent for destination arrival alarms"
                enableVibration(true)
                setBypassDnd(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showAlarmNotification(context: Context, destinationTitle: String, destinationAddress: String) {
        createNotificationChannel(context)

        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmActivity.EXTRA_DESTINATION_TITLE, destinationTitle)
            putExtra(AlarmActivity.EXTRA_DESTINATION_ADDRESS, destinationAddress)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_FULL_SCREEN,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, AlarmStopReceiver::class.java).apply {
            action = AlarmStopReceiver.ACTION_STOP_ALARM
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_STOP,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, HIGH_PRIORITY_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("WAKE UP! Arrived near $destinationTitle")
            .setContentText(destinationAddress)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(
                R.mipmap.ic_launcher,
                "Stop Alarm",
                stopPendingIntent
            )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(HIGH_PRIORITY_NOTIFICATION_ID, builder.build())
    }

    fun cancelAlarmNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(HIGH_PRIORITY_NOTIFICATION_ID)
        notificationManager.cancel(ONGOING_NOTIFICATION_ID)
    }
}
