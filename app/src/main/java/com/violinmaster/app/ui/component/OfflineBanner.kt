package com.violinmaster.app.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.violinmaster.app.data.NetworkMonitor

/**
 * Offline indicator banner that slides in from the top when the device
 * loses internet connectivity and slides out when it reconnects.
 *
 * Place this at the top of your root composable, above all content.
 *
 * REQ-OFFLINE-001: Visual indicator when offline.
 * REQ-OFFLINE-002: Animated show/hide transitions.
 *
 * @param networkMonitor Injected [NetworkMonitor] from Hilt.
 * @param modifier Optional modifier for the banner container.
 */
@Composable
fun OfflineBanner(
    networkMonitor: NetworkMonitor,
    modifier: Modifier = Modifier
) {
    val isOnline by networkMonitor.isOnline.collectAsState(initial = true)

    AnimatedVisibility(
        visible = !isOnline,
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it }),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFB71C1C)) // Deep red
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = "You are offline. Some features may be unavailable.",
                color = Color.White,
                fontSize = 13.sp,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
