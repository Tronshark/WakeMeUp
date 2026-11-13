package com.example.wakemeup.alarm

// Author: TronShark
// Version: v1.0

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AlarmOff
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wakemeup.ui.theme.WakeMeUpTheme

@Composable
fun AlarmScreen(
    destinationTitle: String,
    destinationAddress: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "alarmAnimations")

    // Pulsing animation for outer glow ring
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Pulsing animation for glowing border alpha
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Background Pulsing Glow Circles
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
            )
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scale(pulseScale * 0.9f)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.25f))
            )

            // Main Content Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Badge
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    shadowElevation = 6.dp,
                    modifier = Modifier.padding(top = 32.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Alarm,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "WAKE UP ALERT",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp
                            )
                        )
                    }
                }

                // Middle Glowing Destination Info Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .border(
                            width = 3.dp,
                            color = MaterialTheme.colorScheme.error.copy(alpha = glowAlpha),
                            shape = RoundedCornerShape(28.dp)
                        ),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = "Location",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "You Have Arrived!",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = destinationTitle,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (destinationAddress.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = destinationAddress,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Bottom Actions: Swipe-to-Dismiss Control and Stop Button
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Swipe-To-Dismiss Control
                    SwipeToDismissControl(
                        onDismiss = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Secondary Explicit Stop Button
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Stop,
                            contentDescription = "Stop Alarm",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "STOP ALARM",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SwipeToDismissControl(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                shape = RoundedCornerShape(32.dp)
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        val maxPx = with(density) { (maxWidth - 64.dp).toPx().coerceAtLeast(1f) }
        val animatedOffset by animateFloatAsState(
            targetValue = swipeOffset,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "swipeOffset"
        )

        // Shimmer text indicator
        val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "textAlpha"
        )

        Text(
            text = "Slide to dismiss alarm  ➔",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = alpha)
            ),
            modifier = Modifier.align(Alignment.Center)
        )

        // Swipe Knob
        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.toInt(), 0) }
                .size(64.dp)
                .padding(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error)
                .pointerInput(maxPx) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (swipeOffset > maxPx * 0.75f) {
                                onDismiss()
                            } else {
                                swipeOffset = 0f
                            }
                        },
                        onDragCancel = {
                            swipeOffset = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            swipeOffset = (swipeOffset + dragAmount).coerceIn(0f, maxPx)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.AlarmOff,
                contentDescription = "Swipe Handle",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AlarmScreenPreview() {
    WakeMeUpTheme {
        AlarmScreen(
            destinationTitle = "Grand Central Terminal",
            destinationAddress = "89 E 42nd St, New York, NY 10017",
            onDismiss = {}
        )
    }
}
