package com.example.wakemeup.ui.about

// Author: TronShark
// Version: v1.0

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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
    private val client = OkHttpClient()

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

    /**
     * Sends the comment anonymously to the user's email via Web3Forms relay service.
     *
     * HOW TO GET YOUR FREE ACCESS KEY (Takes 10 seconds):
     * 1. Go to https://web3forms.com
     * 2. Enter your email address: Krrishkrishna612@gmail.com
     * 3. Check your inbox for the Access Key sent by Web3Forms.
     * 4. Replace "YOUR_WEB3FORMS_ACCESS_KEY" below with your actual access key.
     *
     * WHY THIS KEEPS YOUR EMAIL 100% ANONYMOUS:
     * Web3Forms acts as a secure relay service. The app only sends requests with the Access Key token,
     * meaning your raw email address (`Krrishkrishna612@gmail.com`) is never hardcoded or stored anywhere in the APK bytecode,
     * protecting you from spam or reverse-engineering / decompilation.
     */
    suspend fun sendCommentToServer(text: String) {
        withContext(Dispatchers.IO) {
            try {
                val jsonBody = gson.toJson(
                    mapOf(
                        "access_key" to "bd56cd62-758f-4458-926a-ba2bf4b24937",
                        "subject" to "New Comment from WakeMeUp App",
                        "message" to text
                    )
                )
                val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url("https://api.web3forms.com/submit")
                    .post(body)
                    .addHeader("Accept", "application/json")
                    .build()

                client.newCall(request).execute().use { response ->
                    // Response handled
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
