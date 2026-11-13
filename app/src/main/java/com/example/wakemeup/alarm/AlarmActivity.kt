package com.example.wakemeup.alarm

// Author: TronShark
// Version: v1.0

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.wakemeup.ui.theme.WakeMeUpTheme

class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        turnScreenOnAndUnlock()

        val destinationTitle = intent.getStringExtra(EXTRA_DESTINATION_TITLE)
            ?: ActiveAlarmStore.activeDestination.value?.title
            ?: "Destination Reached"
        val destinationAddress = intent.getStringExtra(EXTRA_DESTINATION_ADDRESS)
            ?: ActiveAlarmStore.activeDestination.value?.address
            ?: "You are within your target radius"

        // Ensure alarm sound and vibrator are active
        AlarmSoundVibratorManager.startAlarm(this)

        setContent {
            WakeMeUpTheme {
                AlarmScreen(
                    destinationTitle = destinationTitle,
                    destinationAddress = destinationAddress,
                    onDismiss = {
                        ActiveAlarmStore.stopAlarm(this)
                        finish()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    private fun turnScreenOnAndUnlock() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    companion object {
        const val EXTRA_DESTINATION_TITLE = "extra_destination_title"
        const val EXTRA_DESTINATION_ADDRESS = "extra_destination_address"

        fun launch(context: Context, title: String, address: String) {
            val intent = Intent(context, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_DESTINATION_TITLE, title)
                putExtra(EXTRA_DESTINATION_ADDRESS, address)
            }
            context.startActivity(intent)
        }
    }
}
