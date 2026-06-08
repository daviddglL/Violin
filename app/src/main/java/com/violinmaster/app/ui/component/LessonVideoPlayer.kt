package com.violinmaster.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.violinmaster.app.ui.theme.AppLanguage

@Composable
fun LessonVideoPlayer(
    videoTitle: String,
    videoUrl: String,
    onClose: () -> Unit,
    appLanguage: AppLanguage = AppLanguage.ENGLISH,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        VideoPlayer(
            videoUrl = videoUrl,
            onClose = onClose
        )
    }
}
