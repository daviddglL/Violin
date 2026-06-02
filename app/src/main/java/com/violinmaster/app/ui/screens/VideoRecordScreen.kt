package com.violinmaster.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.violinmaster.app.ui.component.CameraPreview
import com.violinmaster.app.ui.component.RecordingControls
import com.violinmaster.app.ui.component.UploadProgress
import com.violinmaster.app.ui.theme.AppLanguage
import com.violinmaster.app.ui.theme.Localization
import com.violinmaster.app.ui.viewmodel.VideoUploadViewModel
import kotlinx.coroutines.delay

/**
 * Full-screen video recording and upload UI.
 *
 * REQ-VID-009: CameraX PreviewView, record/stop FAB, elapsed timer,
 * upload/discard buttons, compression/upload progress overlays.
 */
@Composable
fun VideoRecordScreen(
    viewModel: VideoUploadViewModel,
    lang: AppLanguage,
    onVideoSent: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val uploadState by viewModel.uploadState.collectAsState()
    val blurWarning by viewModel.blurWarning.collectAsState()

    // Show blur warning banner when blur failed but video was still sent
    LaunchedEffect(blurWarning) {
        if (blurWarning != null) {
            kotlinx.coroutines.delay(8000L)
        }
    }

    // Timer for elapsed seconds during recording
    var elapsedSeconds by remember { mutableStateOf(0) }

    // CAMERA permission handling
    var showRationale by remember { mutableStateOf(false) }
    var isPermanentlyDenied by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRecording()
        } else {
            isPermanentlyDenied = true
        }
    }

    fun checkAndRequestCamera() {
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED -> {
                viewModel.startRecording()
            }
            else -> {
                showRationale = true
            }
        }
    }

    // Permission rationale dialog
    if (showRationale) {
        AlertDialog(
            onDismissRequest = {
                showRationale = false
                onCancel()
            },
            title = {
                Text(Localization.get("video_permission_title", lang))
            },
            text = {
                Text(Localization.get("video_permission_rationale", lang))
            },
            confirmButton = {
                Button(onClick = {
                    showRationale = false
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }) {
                    Text(Localization.get("video_permission_grant", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRationale = false
                    onCancel()
                }) {
                    Text(Localization.get("video_permission_deny", lang))
                }
            }
        )
    }

    // Timer effect during RECORDING
    LaunchedEffect(uploadState) {
        if (uploadState is VideoUploadViewModel.UploadState.Recording) {
            elapsedSeconds = 0
            while (uploadState is VideoUploadViewModel.UploadState.Recording) {
                delay(1000L)
                elapsedSeconds++
                if (elapsedSeconds >= 180) {
                    viewModel.stopRecording()
                    break
                }
            }
        }
    }

    // Main layout
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // Camera preview layer (visible during IDLE and RECORDING)
        val showPreview = uploadState is VideoUploadViewModel.UploadState.Idle ||
            uploadState is VideoUploadViewModel.UploadState.Recording

        if (showPreview) {
            CameraPreview(
                viewModel = viewModel,
                lang = lang,
                onCancel = onCancel
            )
        }

        // State-specific overlays
        when (val state = uploadState) {
            is VideoUploadViewModel.UploadState.Idle -> {
                RecordingControls(
                    state = state,
                    lang = lang,
                    elapsedSeconds = elapsedSeconds,
                    isPermanentlyDenied = isPermanentlyDenied,
                    onRecordClick = { checkAndRequestCamera() },
                    onStopClick = { viewModel.stopRecording() },
                    onCancel = onCancel
                )
            }

            is VideoUploadViewModel.UploadState.Recording -> {
                RecordingControls(
                    state = state,
                    lang = lang,
                    elapsedSeconds = elapsedSeconds,
                    isPermanentlyDenied = isPermanentlyDenied,
                    onRecordClick = { checkAndRequestCamera() },
                    onStopClick = { viewModel.stopRecording() },
                    onCancel = onCancel
                )
            }

            is VideoUploadViewModel.UploadState.Blurring,
            is VideoUploadViewModel.UploadState.Compressing,
            is VideoUploadViewModel.UploadState.Uploading -> {
                UploadProgress(
                    state = state,
                    lang = lang,
                    blurWarning = blurWarning
                )
            }

            is VideoUploadViewModel.UploadState.Done -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(64.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = Localization.get("video_upload_success", lang),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            onVideoSent(state.videoUrl)
                            viewModel.resetState()
                        },
                        modifier = Modifier.fillMaxWidth(0.7f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Localization.get("video_send", lang))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = {
                        viewModel.resetState()
                        onCancel()
                    }) {
                        Text(
                            Localization.get("video_cancel", lang),
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            is VideoUploadViewModel.UploadState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(64.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            viewModel.resetState()
                            checkAndRequestCamera()
                        },
                        modifier = Modifier.fillMaxWidth(0.7f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Localization.get("video_retry", lang))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = {
                        viewModel.resetState()
                        onCancel()
                    }) {
                        Text(
                            Localization.get("video_cancel", lang),
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}
