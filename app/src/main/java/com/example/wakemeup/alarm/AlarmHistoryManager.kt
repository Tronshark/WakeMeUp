package com.example.wakemeup.alarm

// Author: TronShark
// Version: v1.0

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class AlarmHistoryItem(
    val id: String,
    val destinationName: String,
    val timeSetMs: Long,
    var timeRingedMs: Long? = null
)

object AlarmHistoryManager {
    private const val PREFS_NAME = "wakemeup_alarm_history"
    private const val KEY_HISTORY = "history_list"
    private const val TAG = "AlarmHistoryManager"

    private val gson = Gson()
    private val _history = MutableStateFlow<List<AlarmHistoryItem>>(emptyList())
    val history: StateFlow<List<AlarmHistoryItem>> = _history.asStateFlow()

    fun loadHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_HISTORY, null)
        if (json != null) {
            try {
                val type = object : TypeToken<List<AlarmHistoryItem>>() {}.type
                val list: List<AlarmHistoryItem> = gson.fromJson(json, type)
                _history.value = list
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing history", e)
            }
        }
    }

    private fun saveHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(_history.value)
        prefs.edit().putString(KEY_HISTORY, json).apply()
    }

    fun recordAlarmSet(context: Context, destinationName: String): String {
        val newItem = AlarmHistoryItem(
            id = UUID.randomUUID().toString(),
            destinationName = destinationName,
            timeSetMs = System.currentTimeMillis()
        )
        val updated = _history.value.toMutableList()
        updated.add(0, newItem) // Add at top
        _history.value = updated
        saveHistory(context)
        return newItem.id
    }

    fun recordAlarmRing(context: Context, id: String) {
        val updated = _history.value.map {
            if (it.id == id && it.timeRingedMs == null) {
                it.copy(timeRingedMs = System.currentTimeMillis())
            } else {
                it
            }
        }
        _history.value = updated
        saveHistory(context)
    }

    fun clearHistory(context: Context) {
        _history.value = emptyList()
        saveHistory(context)
    }
}
