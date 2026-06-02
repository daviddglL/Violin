package com.violinmaster.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.violinmaster.app.ui.component.NoteTargetSelector
import com.violinmaster.app.ui.component.PitchDisplay
import com.violinmaster.app.ui.viewmodel.TunerViewModel
import com.violinmaster.app.ui.theme.AppLanguage
import com.violinmaster.app.ui.theme.Localization
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TunerScreen(
    viewModel: TunerViewModel,
    appLanguage: AppLanguage,
    modifier: Modifier = Modifier
) {
    val selectedNote by viewModel.tunerSelectedNote.collectAsState()
    val isListening by viewModel.isListeningTuner.collectAsState()
    val pitchOffsetCents by viewModel.tunerPitchOffsetCents.collectAsState()
    val autoDetect by viewModel.tunerAutoDetect.collectAsState()
    val referencePitchA by viewModel.referencePitchA.collectAsState()

    val context = LocalContext.current
    var showPermissionError by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.toggleListeningTuner()
        } else {
            showPermissionError = true
        }
    }

    val onListenClick: () -> Unit = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.toggleListeningTuner()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.selectTunerNote(null)
            if (viewModel.isListeningTuner.value) {
                viewModel.toggleListeningTuner()
            }
        }
    }

    // Smooth needle movement
    val needleAngleOffset by animateFloatAsState(targetValue = pitchOffsetCents, label = "Tuner Needle")

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
            text = Localization.get("smart_tuner_header", appLanguage),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = Localization.get("tuner_subtitle", appLanguage),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // --- Simulated Pitch Gauge Canvas ---
        PitchDisplay(
            selectedNote = selectedNote,
            pitchOffsetCents = pitchOffsetCents,
            isListening = isListening,
            appLanguage = appLanguage,
            needleAngleOffset = needleAngleOffset
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- Toggle Modes (Auto Detect & Microphone Listen) ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = Localization.get("auto_string_detect", appLanguage),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = Localization.get("auto_detect_desc", appLanguage),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = autoDetect,
                onCheckedChange = { viewModel.toggleTunerAutoDetect() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.testTag("auto_detect_switch")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Listening Mode Button ---
        Button(
            onClick = onListenClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("listen_pitch_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isListening) Color(0xFFC53030) else MaterialTheme.colorScheme.primary,
                contentColor = if (isListening) Color.White else MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (isListening) "🔇" else "🎙",
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = Localization.get(
                    if (isListening) "stop_mic_listen" else "start_listen_mode",
                    appLanguage
                ),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall,
                letterSpacing = 1.sp
            )
        }

        // Show permission error when microphone access is denied
        if (showPermissionError) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = Localization.get("mic_permission_error", appLanguage),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Manual Tones Box ---
        Text(
            text = "${Localization.get("play_reference_tone", appLanguage)} (A=$referencePitchA Hz)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        NoteTargetSelector(
            selectedNote = selectedNote,
            isListening = isListening,
            appLanguage = appLanguage,
            onNoteSelected = { viewModel.selectTunerNote(it) }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun BorderStrokeHelper(): androidx.compose.foundation.BorderStroke {
    return androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF49454F))
}
