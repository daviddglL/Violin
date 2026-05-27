package com.violinmaster.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.violinmaster.app.security.VideoSecurityService
import com.violinmaster.app.ui.theme.AppLanguage
import com.violinmaster.app.ui.theme.Localization
import com.violinmaster.app.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

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
            // Case 1: Active Secure Playback Screen
            selectedVideoId != null && securePlaybackUrl != null -> {
                val videoMeta = VideoSecurityService.masterclassVideos.firstOrNull { it.id == selectedVideoId }
                if (videoMeta != null) {
                    SecureMediaPlaybackConsole(
                        videoTitle = videoMeta.title,
                        signedUrl = securePlaybackUrl ?: "",
                        onClose = { authViewModel.closeVideoPlayer() },
                        appLanguage = appLanguage
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

            // Case 3: Setup dynamic passcode if lock is not enabled yet
            !isSecurityLocked -> {
                SecurePasscodeSetupScreen(
                    errorMsg = securityErrorString,
                    onSavePasscode = { pin -> authViewModel.setPasscodeLock(pin) },
                    onResetError = { authViewModel.clearSecurityErrors() },
                    appLanguage = appLanguage
                )
            }

            // Case 4: Unlocked Masterclass Content Hub
            else -> {
                UnlockedMasterclassHub(
                    videos = VideoSecurityService.masterclassVideos,
                    onSelectVideo = { videoId -> authViewModel.selectAndDecryptVideo(videoId) },
                    onRefreshLockState = { authViewModel.logoutUnlockedArea() },
                    appLanguage = appLanguage
                )
            }
        }
    }
}

// ----------------------------------------------------
// SECURE PASSCODE SETUP VIEW
// ----------------------------------------------------
@Composable
fun SecurePasscodeSetupScreen(
    errorMsg: String?,
    onSavePasscode: (String) -> Unit,
    onResetError: () -> Unit,
    appLanguage: AppLanguage = AppLanguage.ENGLISH
) {
    var rawInputPin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Shield Guard",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = Localization.get("secure_student_pin", appLanguage),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = Localization.get("secure_pin_desc", appLanguage),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Large 4-bullet visual representation
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 4) {
                val filled = i < rawInputPin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (filled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Clean tactile security keyboard panel
        SecureKeyboardGrid(
            currentValue = rawInputPin,
            onValueChange = { newVal ->
                if (newVal.length <= 4) {
                    rawInputPin = newVal
                    onResetError()
                }
            },
            onValidate = {
                if (rawInputPin.length == 4) {
                    onSavePasscode(rawInputPin)
                    rawInputPin = ""
                }
            }
        )

        if (errorMsg != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMsg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ----------------------------------------------------
// SECURE AUTH LOGINS SCREEN
// ----------------------------------------------------
@Composable
fun SecureAuthenticationLockScreen(
    errorMsg: String?,
    onSubmitPin: (String) -> Boolean,
    onResetError: () -> Unit,
    onDeconstructLock: () -> Unit,
    appLanguage: AppLanguage = AppLanguage.ENGLISH
) {
    var rawInputPin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Shield Locked",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = Localization.get("enter_vault_pin", appLanguage),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = Localization.get("vault_pin_desc", appLanguage),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Pin bullet displays
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 4) {
                val filled = i < rawInputPin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (filled) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        SecureKeyboardGrid(
            currentValue = rawInputPin,
            onValueChange = { newVal ->
                if (newVal.length <= 4) {
                    rawInputPin = newVal
                    onResetError()
                }
            },
            onValidate = {
                if (rawInputPin.length == 4) {
                    val correct = onSubmitPin(rawInputPin)
                    rawInputPin = ""
                }
            }
        )

        if (errorMsg != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMsg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ----------------------------------------------------
// TACTILE CUSTOM KEYBOARD DESIGN
// ----------------------------------------------------
@Composable
fun SecureKeyboardGrid(
    currentValue: String,
    onValueChange: (String) -> Unit,
    onValidate: () -> Unit
) {
    val keys = listOf(
        "1", "2", "3",
        "4", "5", "6",
        "7", "8", "9",
        "CLR", "0", "OK"
    )

    Column(
        modifier = Modifier.width(260.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val rows = keys.chunked(3)
        rows.forEach { rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowKeys.forEach { key ->
                    val isAction = key == "CLR" || key == "OK"
                    val isOkActive = key == "OK" && currentValue.length == 4

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    isOkActive -> MaterialTheme.colorScheme.primary
                                    isAction -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            )
                            .clickable {
                                when (key) {
                                    "CLR" -> onValueChange("")
                                    "OK" -> onValidate()
                                    else -> {
                                        if (currentValue.length < 4) {
                                            onValueChange(currentValue + key)
                                        }
                                    }
                                }
                            }
                            .testTag("keypad_$key"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isOkActive -> MaterialTheme.colorScheme.onPrimary
                                key == "CLR" -> MaterialTheme.colorScheme.onSurfaceVariant
                                else -> Color.White
                            }
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// UNLOCKED PREMIUM ACADEMY AREA
// ----------------------------------------------------
@Composable
fun UnlockedMasterclassHub(
    videos: List<com.violinmaster.app.security.SecureMasterclassVideo>,
    onSelectVideo: (String) -> Unit,
    onRefreshLockState: () -> Unit,
    appLanguage: AppLanguage = AppLanguage.ENGLISH
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = Localization.get("secure_video_decryption", appLanguage),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = Localization.get("authorized_access_confirmed", appLanguage),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Localization.get("academy_lecture_modules", appLanguage),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Text(
                    text = Localization.get("logout_vault", appLanguage),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onRefreshLockState() }
                        .padding(4.dp)
                )
            }
        }

        // List videos
        items(videos) { video ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = video.difficulty.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = video.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = Localization.get("video_instructor_prefix", appLanguage) + video.mentorName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable { onSelectVideo(video.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Secure Video",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = video.synopsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color.White.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⏱ " + Localization.get("video_duration_prefix", appLanguage) + video.durationString,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "🛡 " + Localization.get("secure_stream_cdn_label", appLanguage),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// SECURE VIDEO PLAYER WINDOW & LOGGER CANVAS
// ----------------------------------------------------
@Composable
fun SecureMediaPlaybackConsole(
    videoTitle: String,
    signedUrl: String,
    onClose: () -> Unit,
    appLanguage: AppLanguage = AppLanguage.ENGLISH
) {
    var playTicks by remember { mutableStateOf(0) }
    var soundwaveBars by remember { mutableStateOf(List(16) { (10..80).random() }) }

    // Soundwave motion simulation during streaming play
    LaunchedEffect(Unit) {
        while (true) {
            delay(150)
            playTicks++
            soundwaveBars = List(16) { (10..90).random() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Video Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Localization.get("active_secure_stream", appLanguage),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = videoTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(Localization.get("close_stream_button", appLanguage), color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Simulated High Fidelity Video Frame Canvas with Audio pulsing waveform
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Live pulsing soundwave to show simulation media rendering activity
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                soundwaveBars.forEach { heightVal ->
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(heightVal.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(
                                    alpha = (heightVal.toFloat() / 100f).coerceIn(0.2f, 1f)
                                )
                            )
                    )
                }
            }

            // Custom Player UI Indicators
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = Localization.get("live_decrypting", appLanguage),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Green,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Color.Red.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                val sec = playTicks % 60
                val min = playTicks / 60
                Text(
                    text = String.format(
                        Localization.get("secure_time_format", appLanguage),
                        min,
                        sec
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // DECRYPT SECURITY DUMP LOGS INFORMATION
        Text(
            text = Localization.get("decrypt_interaction_logs", appLanguage),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Text(
                    text = Localization.get("secure_practice_protects", appLanguage),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))

                val parsedParams = signedUrl.substringAfter("?").split("&")
                val exp = parsedParams.firstOrNull { it.startsWith("expiration=") }?.substringAfter("=") ?: "N/A"
                val nonce = parsedParams.firstOrNull { it.startsWith("nonce=") }?.substringAfter("=") ?: "N/A"
                val ticket = parsedParams.firstOrNull { it.startsWith("signed_ticket=") }?.substringAfter("=") ?: "N/A"

                SecurityLogItem(label = Localization.get("resource_target_protocol", appLanguage), value = signedUrl.substringBefore("?"))
                SecurityLogItem(label = Localization.get("access_ticket_expiry", appLanguage), value = "$exp" + Localization.get("lease_valid", appLanguage))
                SecurityLogItem(label = Localization.get("dynamic_crypto_nonce", appLanguage), value = nonce)
                SecurityLogItem(label = Localization.get("hmac_sha256_signature", appLanguage), value = ticket, isValueCrucial = true)

                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color.Green, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Localization.get("cdn_signature_verified", appLanguage),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Green,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SecurityLogItem(label: String, value: String, isValueCrucial: Boolean = false) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = if (isValueCrucial) MaterialTheme.colorScheme.primary else Color.White,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            maxLines = 1,
            lineHeight = 13.sp
        )
    }
}
