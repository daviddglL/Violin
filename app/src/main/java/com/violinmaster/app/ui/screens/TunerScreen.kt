package com.violinmaster.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.violinmaster.app.domain.model.TuningConfiguration
import com.violinmaster.app.ui.component.NoteTargetSelector
import com.violinmaster.app.ui.component.PitchDisplay
import com.violinmaster.app.ui.viewmodel.TunerViewModel
import com.violinmaster.app.ui.theme.AppLanguage
import com.violinmaster.app.ui.theme.Localization
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    val maxCents by viewModel.maxCents.collectAsState()
    val presets by viewModel.presets.collectAsState()

    val context = LocalContext.current
    var showPermissionError by remember { mutableStateOf(false) }
    var showConfigSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()

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

        // Screen Intro Title + Config Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
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
            }
            IconButton(
                onClick = { showConfigSheet = true },
                modifier = Modifier.testTag("config_button")
            ) {
                Text(
                    text = Localization.get("config_button", appLanguage),
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // --- Simulated Pitch Gauge Canvas ---
        PitchDisplay(
            selectedNote = selectedNote,
            pitchOffsetCents = pitchOffsetCents,
            isListening = isListening,
            appLanguage = appLanguage,
            needleAngleOffset = needleAngleOffset,
            maxCents = maxCents
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
                text = if (isListening) "\uD83D\uDD07" else "\uD83C\uDF99",
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

    // ── Configuration Bottom Sheet ──
    if (showConfigSheet) {
        ModalBottomSheet(
            onDismissRequest = { showConfigSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            ConfigSheetContent(
                referencePitchA = referencePitchA,
                maxCents = maxCents,
                presets = presets,
                appLanguage = appLanguage,
                onReferencePitchChange = { viewModel.updateReferencePitch(it) },
                onMaxCentsChange = { viewModel.updateMaxCents(it) },
                onSavePreset = { label -> viewModel.saveCurrentAsPreset(label) },
                onLoadPreset = { label -> viewModel.loadPreset(label) },
                onDeletePreset = { label -> viewModel.deletePreset(label) },
                onClose = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showConfigSheet = false
                    }
                }
            )
        }
    }
}

// ── Available max-cents options ──
private val MAX_CENTS_OPTIONS = listOf(25, 50, 75, 100, 150, 200)

@Composable
private fun ConfigSheetContent(
    referencePitchA: Int,
    maxCents: Int,
    presets: List<TuningConfiguration>,
    appLanguage: AppLanguage,
    onReferencePitchChange: (Int) -> Unit,
    onMaxCentsChange: (Int) -> Unit,
    onSavePreset: (String) -> Unit,
    onLoadPreset: (String) -> Unit,
    onDeletePreset: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var presetLabel by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        // ── Header ──
        Text(
            text = Localization.get("config_sheet_title", appLanguage),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
        )

        // ── Reference Pitch Slider ──
        Text(
            text = "${Localization.get("config_reference_pitch", appLanguage)}: $referencePitchA Hz",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "350",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = referencePitchA.toFloat(),
                onValueChange = { onReferencePitchChange(it.toInt()) },
                valueRange = 350f..500f,
                steps = 0,
                modifier = Modifier
                    .weight(1f)
                    .testTag("reference_pitch_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            Text(
                text = "500",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Max Cents Selector ──
        Text(
            text = "${Localization.get("config_max_cents", appLanguage)}: ±$maxCents",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MAX_CENTS_OPTIONS.forEach { option ->
                val isSelected = maxCents == option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color(0xFF49454F),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onMaxCentsChange(option) }
                        .testTag("max_cents_option_$option"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Save Preset ──
        Text(
            text = Localization.get("config_save_preset", appLanguage),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = presetLabel,
                onValueChange = { presetLabel = it },
                placeholder = {
                    Text(
                        text = Localization.get("config_save_hint", appLanguage),
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("preset_name_input"),
                textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White)
            )
            Button(
                onClick = {
                    if (presetLabel.isNotBlank()) {
                        onSavePreset(presetLabel.trim())
                        presetLabel = ""
                    }
                },
                enabled = presetLabel.isNotBlank(),
                modifier = Modifier.testTag("save_preset_button"),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = Localization.get("config_save_preset", appLanguage),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── My Presets ──
        Text(
            text = Localization.get("config_my_presets", appLanguage),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (presets.isEmpty()) {
            Text(
                text = Localization.get("config_no_presets", appLanguage),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .testTag("presets_list")
            ) {
                items(presets, key = { it.label }) { preset ->
                    PresetRow(
                        preset = preset,
                        appLanguage = appLanguage,
                        onLoad = { onLoadPreset(preset.label) },
                        onDelete = { onDeletePreset(preset.label) }
                    )
                }
            }
        }

        // ── Close Button ──
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("close_config_sheet"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = Localization.get("config_close", appLanguage),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun PresetRow(
    preset: TuningConfiguration,
    appLanguage: AppLanguage,
    onLoad: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = preset.label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "A=${preset.referencePitch} Hz  \u00B1${preset.maxCents}\u00A2",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(
            onClick = onLoad,
            modifier = Modifier.testTag("load_preset_${preset.label}")
        ) {
            Text(
                text = Localization.get("config_load", appLanguage),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        TextButton(
            onClick = onDelete,
            modifier = Modifier.testTag("delete_preset_${preset.label}")
        ) {
            Text(
                text = Localization.get("config_delete", appLanguage),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFC53030)
            )
        }
    }
}

@Composable
fun BorderStrokeHelper(): androidx.compose.foundation.BorderStroke {
    return androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF49454F))
}
