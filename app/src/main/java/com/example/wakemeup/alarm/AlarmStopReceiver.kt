package com.example.wakemeup.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmStopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_STOP_ALARM) {
            Log.i(TAG, "Alarm stop action received via broadcast")
            ActiveAlarmStore.stopAlarm(context)
        }
    }

    companion object {
        private const val TAG = "AlarmStopReceiver"
        const val ACTION_STOP_ALARM = "com.example.wakemeup.ACTION_STOP_ALARM"
    }
}
