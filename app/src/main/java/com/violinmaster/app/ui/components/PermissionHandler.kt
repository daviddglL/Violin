package com.violinmaster.app.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

/**
 * Composable wrapper that guards content behind [android.Manifest.permission.RECORD_AUDIO].
 *
 * Shows a rationale dialog before requesting permission. Displays error state
 * when permission is permanently denied.
 *
 * Usage:
 * ```kotlin
 * PermissionHandler(
 *     onPermissionGranted = { tunerViewModel.startListening() },
 *     onPermissionDenied = { /* show error */ }
 * ) {
 *     // Your listening-mode UI here
 * }
 * ```
 *
 * @param onPermissionGranted Called when RECORD_AUDIO is granted (or already granted)
 * @param onPermissionDenied Called when permission is denied or unavailable
 * @param content Composable content to render when permission is granted
 */
@Composable
fun PermissionHandler(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var showRationale by remember { mutableStateOf(false) }
    var isPermanentlyDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onPermissionGranted()
        } else {
            isPermanentlyDenied = true
            onPermissionDenied?.invoke()
        }
    }

    fun checkAndRequest() {
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED -> {
                // Already granted
                onPermissionGranted()
            }
            else -> {
                // Show rationale first, then request
                showRationale = true
            }
        }
    }

    // Permission already granted — render content directly
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    ) {
        content()
        return
    }

    // Permission not granted — show error or rationale
    if (isPermanentlyDenied) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Microphone permission required",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "The tuner needs microphone access to detect pitch. " +
                    "Please grant permission in your device Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // Show rationale dialog
    if (showRationale) {
        AlertDialog(
            onDismissRequest = {
                showRationale = false
                onPermissionDenied?.invoke()
            },
            title = {
                Text("Microphone Access Needed")
            },
            text = {
                Text(
                    "The tuner uses your device microphone to detect the pitch of " +
                        "your violin strings. No audio is recorded or stored."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showRationale = false
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }) {
                    Text("Grant Access")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRationale = false
                    onPermissionDenied?.invoke()
                }) {
                    Text("Not Now")
                }
            }
        )
    }
}
