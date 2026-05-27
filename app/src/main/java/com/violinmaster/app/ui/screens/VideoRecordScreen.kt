package com.violinmaster.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.violinmaster.app.ui.theme.AppLanguage
import com.violinmaster.app.ui.theme.Localization
import com.violinmaster.app.ui.viewmodel.VideoUploadViewModel
import kotlinx.coroutines.delay

/**
 * Full-screen video recording and upload UI.
 *
 * REQ-VID-009: CameraX PreviewView, record/stop FAB, elapsed timer,
 * upload/discard buttons, compression/upload progress overlays.
 *
 * States handled:
 * - IDLE: Camera preview + record button
 * - RECORDING: Preview + blinking indicator + timer + stop button
 * - COMPRESSING: Overlay with progress bar
 * - UPLOADING: Overlay with progress bar
 * - DONE: Success message + "Send to Chat" / cancel buttons
 * - ERROR: Error message + retry / cancel buttons
 *
 * @param viewModel VideoUploadViewModel controlling the pipeline.
 * @param lang Current app language for localization.
 * @param onVideoSent Called when user taps "Send to Chat" with the download URL.
 * @param onCancel Called when user cancels or navigates back.
 */
@Composable
fun VideoRecordScreen(
    viewModel: VideoUploadViewModel,
    lang: AppLanguage,
    onVideoSent: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uploadState by viewModel.uploadState.collectAsState()
    val blurWarning by viewModel.blurWarning.collectAsState()

    // Show blur warning banner when blur failed but video was still sent
    LaunchedEffect(blurWarning) {
        if (blurWarning != null) {
            // Clear after 8 seconds
            kotlinx.coroutines.delay(8000L)
            // Reset is handled by resetState when the user exits
        }
    }

    // Timer for elapsed seconds during recording
    var elapsedSeconds by remember { mutableStateOf(0) }

    // CAMERA permission handling — mirrors PermissionHandler pattern
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

    // ── Permission rationale dialog ────────────────────────────────────
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

    // ── Timer effect during RECORDING ──────────────────────────────────
    LaunchedEffect(uploadState) {
        if (uploadState is VideoUploadViewModel.UploadState.Recording) {
            elapsedSeconds = 0
            while (uploadState is VideoUploadViewModel.UploadState.Recording) {
                delay(1000L)
                elapsedSeconds++
                // Auto-stop at 3-minute limit (180s)
                if (elapsedSeconds >= 180) {
                    viewModel.stopRecording()
                    break
                }
            }
        }
    }

    // ── Main layout ────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // Camera preview (visible during IDLE and RECORDING)
        val showPreview = uploadState is VideoUploadViewModel.UploadState.Idle ||
            uploadState is VideoUploadViewModel.UploadState.Recording

        if (showPreview) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        // Bind CameraX lifecycle when the view is attached
                        post {
                            viewModel.sessionManager.let {
                                // CameraX binding happens via recordingService
                                // which requires explicit bindToLifecycle call
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // ── Top bar with cancel button ─────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .statusBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        if (viewModel.isRecording()) {
                            viewModel.cancelRecording()
                        }
                        onCancel()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = Localization.get("video_cancel", lang),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ── State-specific overlays ────────────────────────────────────
        when (val state = uploadState) {
            is VideoUploadViewModel.UploadState.Idle -> {
                // Record button at bottom center
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Check permission first
                    if (isPermanentlyDenied) {
                        Text(
                            text = Localization.get("video_permission_required", lang),
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                        Button(onClick = onCancel) {
                            Text(Localization.get("back_button", lang))
                        }
                    } else {
                        // Big red record button
                        Button(
                            onClick = { checkAndRequestCamera() },
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color.White, CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = Localization.get("video_record", lang),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            is VideoUploadViewModel.UploadState.Recording -> {
                // ── Recording indicator + timer ────────────────────────
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Blinking red dot indicator
                    var showDot by remember { mutableStateOf(true) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            showDot = !showDot
                            delay(500L)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnimatedVisibility(visible = showDot) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color.Red, CircleShape)
                            )
                        }

                        Text(
                            text = formatElapsed(elapsedSeconds),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = Localization.get("video_recording", lang),
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )

                    // 3-minute countdown warning
                    if (elapsedSeconds >= 150) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${Localization.get("video_time_remaining", lang)} ${formatElapsed(180 - elapsedSeconds)}",
                            color = if (elapsedSeconds >= 170) Color.Red else Color.Yellow,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // ── Stop button at bottom ──────────────────────────────
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = { viewModel.stopRecording() },
                        modifier = Modifier.size(80.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp, 24.dp)
                                .background(Color.White, RoundedCornerShape(4.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = Localization.get("video_stop", lang),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            is VideoUploadViewModel.UploadState.Blurring -> {
                // ── Dark overlay with blur progress ────────────────────
                // REQ-BLR-005: Show "Protecting identity..." + frame progress
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        // Shield icon for privacy protection
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF4FC3F7),
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = Localization.get("blur_processing", lang),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Frame progress text
                        if (state.progress.isNotEmpty()) {
                            val isProtecting = state.progress == "Protecting identity..."
                            Text(
                                text = if (isProtecting) {
                                    Localization.get("blur_processing", lang)
                                } else {
                                    state.progress
                                },
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Indeterminate progress indicator during blur
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = Color(0xFF4FC3F7),
                            trackColor = Color.White.copy(alpha = 0.15f),
                            strokeWidth = 4.dp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Privacy protection active",
                            color = Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // ── Blur warning banner ────────────────────────────────
                AnimatedVisibility(
                    visible = blurWarning != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = blurWarning ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            is VideoUploadViewModel.UploadState.Compressing -> {
                // ── Dark overlay with compression progress ─────────────
                ProcessingOverlay(
                    title = Localization.get("video_compressing", lang),
                    progress = state.progress,
                    lang = lang
                )
            }

            is VideoUploadViewModel.UploadState.Uploading -> {
                // ── Dark overlay with upload progress ──────────────────
                ProcessingOverlay(
                    title = Localization.get("video_uploading", lang),
                    progress = state.progress,
                    lang = lang
                )
            }

            is VideoUploadViewModel.UploadState.Done -> {
                // ── Success message + actions ──────────────────────────
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
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
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
                // ── Error message + retry ──────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Error,
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

/**
 * Reusable processing overlay with title and progress bar.
 * Used for both COMPRESSING and UPLOADING states.
 */
@Composable
private fun ProcessingOverlay(
    title: String,
    progress: Float,
    lang: AppLanguage
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(64.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.15f),
                strokeWidth = 4.dp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${(progress * 100).toInt()}%",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.15f)
            )
        }
    }
}

/**
 * Formats elapsed seconds as MM:SS.
 *
 * @param totalSeconds Total elapsed seconds.
 * @return Formatted string like "02:35" or "00:07".
 */
private fun formatElapsed(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
