package com.example.wakemeup

// Author: TronShark
// Version: v1.0

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.example.wakemeup.alarm.ActiveAlarmStore
import com.example.wakemeup.alarm.AlarmActivity
import com.example.wakemeup.ui.map.MapScreen
import com.example.wakemeup.ui.about.AboutScreen
import com.example.wakemeup.ui.theme.WakeMeUpTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateListOf

private data object MapScreenRoute
private data object AboutScreenRoute

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Notification permission result handled if necessary
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        ActiveAlarmStore.restoreState(this)

        checkAndRequestNotificationPermission()

        // Observe alarm trigger state to launch full-screen AlarmActivity when active
        lifecycleScope.launch {
            ActiveAlarmStore.isAlarmTriggered.collect { isTriggered ->
                if (isTriggered) {
                    val activeDest = ActiveAlarmStore.activeDestination.value
                    AlarmActivity.launch(
                        this@MainActivity,
                        title = activeDest?.title ?: "Destination Reached",
                        address = activeDest?.address ?: "Arrived within target radius"
                    )
                }
            }
        }

        setContent {
            WakeMeUpTheme {
                val backStack = remember { mutableStateListOf<Any>(MapScreenRoute) }
                var isLoading by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    delay(800)
                    isLoading = false
                }

                Crossfade(
                    targetState = isLoading,
                    animationSpec = tween(durationMillis = 400),
                    label = "SplashTransition"
                ) { loading ->
                    if (loading) {
                        LoadingScreen()
                    } else {
                        NavDisplay(
                            backStack = backStack,
                            onBack = { backStack.removeLastOrNull() },
                            entryProvider = { key ->
                                when(key) {
                                    is MapScreenRoute -> NavEntry(key) {
                                        MapScreen(
                                            modifier = Modifier.fillMaxSize(),
                                            onNavigateToAbout = { backStack.add(AboutScreenRoute) }
                                        )
                                    }
                                    is AboutScreenRoute -> NavEntry(key) {
                                        AboutScreen(
                                            onNavigateBack = { backStack.removeLastOrNull() },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    else -> error("Unknown route")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_splash_branding),
                contentDescription = "WakeMeUp Logo",
                modifier = Modifier.size(220.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            CircularProgressIndicator(
                color = Color(0xFF38BDF8),
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
