package com.violinmaster.app.ui.component

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.violinmaster.app.ui.theme.AppLanguage
import com.violinmaster.app.ui.theme.Localization
import com.violinmaster.app.ui.viewmodel.VideoUploadViewModel

@Composable
fun CameraPreview(
    viewModel: VideoUploadViewModel,
    lang: AppLanguage,
    onCancel: () -> Unit
) {
    // Camera preview (visible during IDLE and RECORDING)
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                // Bind CameraX lifecycle when the view is attached
                post {
                    viewModel.authManager.let {
                        // CameraX binding happens via recordingService
                        // which requires explicit bindToLifecycle call
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )

    // Top bar with cancel button
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
