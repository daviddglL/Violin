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

    val stringFrequencies = mapOf(
        "G" to "196.0 Hz",
        "D" to "293.7 Hz",
        "A" to "440.0 Hz",
        "E" to "659.3 Hz"
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
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .testTag("tuner_gauge_container"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            border = BorderStrokeHelper()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Dial Graphic
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height

                        val centerX = width / 2f
                        val centerY = height * 0.85f
                        val needleLength = height * 0.7f
                        val arcRadius = height * 0.65f

                        // Draw Background Arc
                        drawArc(
                            color = Color(0xFF49454F),
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = Offset(centerX - arcRadius, centerY - arcRadius),
                            size = size.copy(width = arcRadius * 2, height = arcRadius * 2),
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Draw tick marks inside the arc
                        // -50 cents to 50 cents (11 lines from 180deg to 360deg, steps of 18deg)
                        for (i in 0..10) {
                            val stepAngle = 180f + i * 18f
                            val angleRad = Math.toRadians(stepAngle.toDouble())

                            val startRadius = arcRadius - 8.dp.toPx()
                            val endRadius = arcRadius + 8.dp.toPx()

                            val startX = centerX + startRadius * cos(angleRad).toFloat()
                            val startY = centerY + startRadius * sin(angleRad).toFloat()
                            val endX = centerX + endRadius * cos(angleRad).toFloat()
                            val endY = centerY + endRadius * sin(angleRad).toFloat()

                            val color = when (i) {
                                0, 10 -> Color(0xFFE53935) // Flat/Sharp warnings
                                5 -> Color(0xFF81C784) // Perfectly in Tune
                                else -> Color(0xFF938F99)
                            }
                            val thickness = if (i % 5 == 0) 4.dp.toPx() else 1.5.dp.toPx()

                            drawLine(
                                color = color,
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = thickness,
                                cap = StrokeCap.Round
                            )
                        }

                        // Drawing text ticks
                        // -50 cents (flat), In Tune (0), +50 cents (sharp)

                        // Draw sweeping analog needle
                        // needleAngleOffset goes from -50 (extreme flat) to +50 (extreme sharp).
                        // Let's map it to an angle between 210 degrees and 330 degrees (center is 270)
                        val angleMap = 270f + (needleAngleOffset / 50f) * 60f
                        val needleRad = Math.toRadians(angleMap.toDouble())

                        val endNeedleX = centerX + needleLength * cos(needleRad).toFloat()
                        val endNeedleY = centerY + needleLength * sin(needleRad).toFloat()

                        val isPerfect = Math.abs(pitchOffsetCents) < 2.5f && isListening

                        drawLine(
                            color = if (isPerfect) Color(0xFF81C784) else Color(0xFFD0BCFF),
                            start = Offset(centerX, centerY),
                            end = Offset(endNeedleX, endNeedleY),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Center Pivot point
                        drawCircle(
                            color = if (isPerfect) Color(0xFF81C784) else Color(0xFFEADDFF),
                            radius = 8.dp.toPx(),
                            center = Offset(centerX, centerY)
                        )
                        drawCircle(
                            color = Color(0xFF1C1B1F),
                            radius = 3.dp.toPx(),
                            center = Offset(centerX, centerY)
                        )
                    }

                    // Floating Big Selected Note Label
                    Column(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = selectedNote ?: "--",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (Math.abs(pitchOffsetCents) < 2.5f && isListening) Color(0xFF81C784) else Color.White,
                            fontFamily = FontFamily.Serif
                        )
                        val isPerfect = Math.abs(pitchOffsetCents) < 2.5f && isListening
                        Text(
                            text = if (!isListening && selectedNote == null) {
                                Localization.get("select_string_prompt", appLanguage)
                            } else if (!isListening) {
                                Localization.get("reference_playback", appLanguage)
                            } else if (isPerfect) {
                                Localization.get("perfectly_in_tune", appLanguage)
                            } else {
                                String.format(
                                    "%.1f cents %s",
                                    Math.abs(pitchOffsetCents),
                                    Localization.get(
                                        if (pitchOffsetCents < 0) "cents_flat" else "cents_sharp",
                                        appLanguage
                                    )
                                )
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isPerfect) Color(0xFF81C784) else MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val noteOrder = listOf("G", "D", "A", "E")
            for (note in noteOrder) {
                val isSelected = selectedNote == note && !isListening
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        )
                        .clickable {
                            if (isSelected) {
                                viewModel.selectTunerNote(null) // stop continuous
                            } else {
                                viewModel.selectTunerNote(note)
                            }
                        }
                        .border(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF49454F),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .testTag("string_note_button_$note"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = note,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringFrequencies[note] ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (selectedNote != null && !isListening) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { viewModel.selectTunerNote(null) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(36.dp).align(Alignment.CenterHorizontally).testTag("stop_string_sound")
            ) {
                Text("🔇", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(Localization.get("stop_synth_sound", appLanguage), style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun BorderStrokeHelper(): androidx.compose.foundation.BorderStroke {
    return androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF49454F))
}
