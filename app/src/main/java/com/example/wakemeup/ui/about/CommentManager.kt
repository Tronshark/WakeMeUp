package com.example.wakemeup.ui.about

// Author: TronShark
// Version: v1.0

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

data class UserComment(
    val id: String,
    val text: String,
    val timestamp: Long
)

class CommentManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("comments_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val COMMENTS_KEY = "comments_list"

    fun getComments(): List<UserComment> {
        val json = prefs.getString(COMMENTS_KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<UserComment>>() {}.type
        return gson.fromJson(json, type)
    }

    fun addComment(text: String) {
        val currentComments = getComments().toMutableList()
        val newComment = UserComment(
            id = UUID.randomUUID().toString(),
            text = text,
            timestamp = System.currentTimeMillis()
        )
        currentComments.add(0, newComment) // Add to the top
        val json = gson.toJson(currentComments)
        prefs.edit().putString(COMMENTS_KEY, json).apply()
    }
}
