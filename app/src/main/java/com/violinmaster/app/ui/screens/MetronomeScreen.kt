package com.violinmaster.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.violinmaster.app.ui.component.BeatIndicator
import com.violinmaster.app.ui.component.BpmDial
import com.violinmaster.app.ui.component.TimeSignatureSelector
import com.violinmaster.app.ui.viewmodel.MetronomeViewModel
import com.violinmaster.app.ui.theme.AppLanguage
import com.violinmaster.app.ui.theme.Localization

@Composable
fun MetronomeScreen(
    viewModel: MetronomeViewModel,
    appLanguage: AppLanguage,
    modifier: Modifier = Modifier
) {
    val bpm by viewModel.metronomeBpm.collectAsState()
    val beats by viewModel.metronomeBeats.collectAsState()
    val accent by viewModel.metronomeAccent.collectAsState()
    val isPlaying by viewModel.isMetronomePlaying.collectAsState()
    val activeBeatPulse by viewModel.metronomeBeatPulse.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            if (viewModel.isMetronomePlaying.value) {
                viewModel.toggleMetronome()
            }
        }
    }

    // Tactile Tap Tempo recording timestamps
    val tapTimestamps = remember { mutableStateListOf<Long>() }

    fun registerTapTempo() {
        val now = System.currentTimeMillis()
        tapTimestamps.add(now)
        // Keep only past 5 timestamps
        if (tapTimestamps.size > 5) {
            tapTimestamps.removeAt(0)
        }
        if (tapTimestamps.size >= 2) {
            val intervals = mutableListOf<Long>()
            for (i in 1 until tapTimestamps.size) {
                intervals.add(tapTimestamps[i] - tapTimestamps[i - 1])
            }
            // Average interval in ms
            val avgInterval = intervals.average()
            if (avgInterval > 0) {
                // translate interval in ms to bpm (e.g. 500ms -> 120bpm)
                val computedBpm = (60000.0 / avgInterval).toInt().coerceIn(40, 240)
                viewModel.updateMetronomeBpm(computedBpm)
            }
        }
    }

    // Dynamic scale animate on pulse beat
    val scaleFactor by animateFloatAsState(
        targetValue = if (activeBeatPulse != -1) 1.15f else 1.0f,
        animationSpec = spring(stiffness = 300f, dampingRatio = 0.5f),
        label = "Beat Pulse Animation"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Screen Intro Title
        Text(
            text = Localization.get("metronome_header", appLanguage),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = Localization.get("metronome_subtitle", appLanguage),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // --- Large Tactile Visual Pulse Node ---
        BeatIndicator(
            scaleFactor = scaleFactor,
            activeBeatPulse = activeBeatPulse,
            beats = beats,
            accent = accent,
            appLanguage = appLanguage
        )

        Spacer(modifier = Modifier.height(20.dp))

        // --- Tactile BPM Slider & Dragging ---
        BpmDial(
            bpm = bpm,
            appLanguage = appLanguage,
            onBpmChange = { viewModel.updateMetronomeBpm(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- Buttons panel: START / PLAY and TAP TEMPO ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.toggleMetronome() },
                modifier = Modifier
                    .weight(1.2f)
                    .height(56.dp)
                    .testTag("toggle_metronome_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPlaying) Color(0xFFC53030) else MaterialTheme.colorScheme.primary,
                    contentColor = if (isPlaying) Color.White else MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isPlaying) {
                    Text("⏸", modifier = Modifier.padding(end = 4.dp), color = Color.White, fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = if (isPlaying) Localization.get("pause_pulse", appLanguage) else Localization.get("play_metronome", appLanguage),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall,
                    letterSpacing = 0.5.sp
                )
            }

            Button(
                onClick = { registerTapTempo() },
                modifier = Modifier
                    .weight(0.8f)
                    .height(56.dp)
                    .testTag("tap_tempo_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStrokeHelper()
            ) {
                Text(
                    text = Localization.get("tap_tempo_button", appLanguage),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Time Division Settings Cards ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Beat Division selection card
            TimeSignatureSelector(
                modifier = Modifier.weight(1f),
                beats = beats,
                appLanguage = appLanguage,
                onBeatsChange = { viewModel.updateMetronomeBeats(it) }
            )

            // Downbeat Accent card
            Card(
                modifier = Modifier
                    .weight(0.8f)
                    .height(110.dp)
                    .testTag("accent_selector_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStrokeHelper()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .clickable { viewModel.toggleMetronomeAccent() },
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = Localization.get("downbeat_accent_label", appLanguage),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (accent) Localization.get("accent_on", appLanguage) else Localization.get("accent_off", appLanguage),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (accent) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = if (accent) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
