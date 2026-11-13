package com.example.wakemeup.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.location.Location
import android.view.View
import android.widget.RemoteViews
import com.example.wakemeup.MainActivity
import com.example.wakemeup.R
import com.example.wakemeup.alarm.ActiveAlarmStore
import com.example.wakemeup.alarm.AlarmNotificationHelper
import com.example.wakemeup.alarm.AlarmStopReceiver
import com.example.wakemeup.location.LocationForegroundService

class WakeMeUpWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        private const val REQUEST_CODE_STOP_WIDGET = 4001
        private const val REQUEST_CODE_OPEN_APP_WIDGET = 4002

        fun updateWidget(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, WakeMeUpWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_alarm_status)
            val activeDest = ActiveAlarmStore.activeDestination.value
            val location = LocationForegroundService.currentLocation.value

            if (activeDest != null) {
                views.setViewVisibility(R.id.widget_active_container, View.VISIBLE)
                views.setViewVisibility(R.id.widget_empty_container, View.GONE)
                views.setImageViewResource(R.id.widget_status_dot, R.drawable.widget_status_dot_active)
                views.setTextViewText(R.id.widget_destination_title, activeDest.title)

                if (location != null) {
                    val results = FloatArray(1)
                    Location.distanceBetween(
                        location.latitude, location.longitude,
                        activeDest.latitude, activeDest.longitude,
                        results
                    )
                    val distanceMeters = results[0]
                    val formattedDistance = AlarmNotificationHelper.formatDistance(distanceMeters)
                    val speedKmH = AlarmNotificationHelper.calculateSpeedKmH(location)
                    val etaMins = AlarmNotificationHelper.calculateEtaMins(location, distanceMeters)
                    val formattedEta = AlarmNotificationHelper.formatEta(etaMins)

                    views.setTextViewText(R.id.widget_distance_value, "📏 $formattedDistance")
                    views.setTextViewText(R.id.widget_speed_eta_value, "⚡ $speedKmH km/h • ⏱️ $formattedEta")
                } else {
                    views.setTextViewText(R.id.widget_distance_value, "📏 Calculating...")
                    views.setTextViewText(R.id.widget_speed_eta_value, "⚡ -- km/h • ⏱️ --")
                }
            } else {
                views.setViewVisibility(R.id.widget_active_container, View.GONE)
                views.setViewVisibility(R.id.widget_empty_container, View.VISIBLE)
                views.setImageViewResource(R.id.widget_status_dot, R.drawable.widget_status_dot_inactive)
                views.setTextViewText(R.id.widget_empty_text, "No Active Alarm • Open app to set stop")
            }

            // Stop Alarm PendingIntent
            val stopIntent = Intent(context, AlarmStopReceiver::class.java).apply {
                action = AlarmStopReceiver.ACTION_STOP_ALARM
            }
            val stopPendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_STOP_WIDGET,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_stop_alarm, stopPendingIntent)

            // Click root / empty state to open MainActivity
            val mainIntent = Intent(context, MainActivity::class.java)
            val mainPendingIntent = PendingIntent.getActivity(
                context,
                REQUEST_CODE_OPEN_APP_WIDGET,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, mainPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_empty_container, mainPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
