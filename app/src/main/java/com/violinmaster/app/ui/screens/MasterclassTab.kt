package com.violinmaster.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.violinmaster.app.security.VideoSecurityService
import com.violinmaster.app.ui.component.SecureAuthenticationLockScreen
import com.violinmaster.app.ui.component.UnlockedMasterclassHub
import com.violinmaster.app.ui.component.VideoPlayer
import com.violinmaster.app.ui.theme.AppLanguage
import com.violinmaster.app.ui.viewmodel.AuthViewModel

@Composable
fun MasterclassTab(
    authViewModel: AuthViewModel,
    appLanguage: AppLanguage = AppLanguage.ENGLISH,
    modifier: Modifier = Modifier
) {
    val isSecurityLocked by authViewModel.isSecurityLocked.collectAsState()
    val isUserAuthenticated by authViewModel.isUserAuthenticated.collectAsState()
    val securityErrorString by authViewModel.securityErrorString.collectAsState()
    val selectedVideoId by authViewModel.selectedPremiumVideoId.collectAsState()
    val securePlaybackUrl by authViewModel.securePlaybackUrl.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            // Case 1: Active Video Playback — real ExoPlayer via VideoPlayer composable
            selectedVideoId != null && securePlaybackUrl != null -> {
                val videoMeta = VideoSecurityService.masterclassVideos
                    .firstOrNull { it.id == selectedVideoId }
                if (videoMeta != null) {
                    VideoPlayer(
                        videoUrl = securePlaybackUrl ?: "",
                        onClose = { authViewModel.closeVideoPlayer() }
                    )
                }
            }

            // Case 2: Locked screen waiting for PIN code auth
            isSecurityLocked && !isUserAuthenticated -> {
                SecureAuthenticationLockScreen(
                    errorMsg = securityErrorString,
                    onSubmitPin = { pin -> authViewModel.authenticatePasscode(pin) },
                    onResetError = { authViewModel.clearSecurityErrors() },
                    onDeconstructLock = {
                        authViewModel.logoutUnlockedArea()
                    },
                    appLanguage = appLanguage
                )
            }

            // Case 3: No passcode set — show unlocked hub directly (no mandatory passcode wall)
            !isSecurityLocked -> {
                UnlockedMasterclassHub(
                    videos = VideoSecurityService.masterclassVideos,
                    onSelectVideo = { videoId ->
                        authViewModel.selectAndDecryptVideo(videoId)
                    },
                    onRefreshLockState = { authViewModel.logoutUnlockedArea() },
                    appLanguage = appLanguage,
                    modifier = Modifier.testTag("masterclass_content_hub")
                )
            }

            // Case 4: Unlocked Masterclass Content Hub (passcode authenticated)
            else -> {
                UnlockedMasterclassHub(
                    videos = VideoSecurityService.masterclassVideos,
                    onSelectVideo = { videoId ->
                        authViewModel.selectAndDecryptVideo(videoId)
                    },
                    onRefreshLockState = { authViewModel.logoutUnlockedArea() },
                    appLanguage = appLanguage,
                    modifier = Modifier.testTag("masterclass_content_hub")
                )
            }
        }
    }
}
