package com.violinmaster.app.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.violinmaster.app.ui.theme.AppLanguage
import com.violinmaster.app.ui.theme.Localization
import com.violinmaster.app.ui.viewmodel.VideoUploadViewModel
import kotlinx.coroutines.delay

@Composable
fun RecordingControls(
    state: VideoUploadViewModel.UploadState,
    lang: AppLanguage,
    elapsedSeconds: Int,
    isPermanentlyDenied: Boolean,
    onRecordClick: () -> Unit,
    onStopClick: () -> Unit,
    onCancel: () -> Unit
) {
    when (state) {
        is VideoUploadViewModel.UploadState.Idle -> {
            // Record button at bottom center
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 48.dp)
                ) {
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
                        Button(
                            onClick = onRecordClick,
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
        }

        is VideoUploadViewModel.UploadState.Recording -> {
            Box(modifier = Modifier.fillMaxSize()) {
                // Recording indicator + timer at top
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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

                // Stop button at bottom
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = onStopClick,
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
        }

        else -> { /* Other states handled by parent */ }
    }
}

/**
 * Formats elapsed seconds as MM:SS.
 */
private fun formatElapsed(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
