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
  signedUrl: String,
  onClose: () -> Unit,
  appLanguage: AppLanguage,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    SecureMediaPlaybackConsole(
      videoTitle = videoTitle,
      signedUrl = signedUrl,
      onClose = onClose,
      appLanguage = appLanguage
    )
  }
}
