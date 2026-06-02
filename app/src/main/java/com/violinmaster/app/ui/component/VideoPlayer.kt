package com.violinmaster.app.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.violinmaster.app.ui.theme.AppLanguage
import com.violinmaster.app.ui.theme.Localization
import kotlinx.coroutines.delay

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
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
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
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
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
