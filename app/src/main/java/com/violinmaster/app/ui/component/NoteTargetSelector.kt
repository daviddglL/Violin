package com.violinmaster.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.violinmaster.app.domain.model.Instrument
import com.violinmaster.app.domain.model.InstrumentString
import com.violinmaster.app.ui.theme.AppLanguage
import com.violinmaster.app.ui.theme.Localization
import kotlin.math.log2
import kotlin.math.roundToInt

/**
 * Derive a display label with octave suffix from an instrument string definition.
 *
 * Uses the MIDI note formula (A4 = 69) to determine the octave number via truncation
 * (MIDI 0 = C-1, MIDI 12 = C0, ..., MIDI 60 = C4). Produces labels like
 * "G3", "D4", "A4", "E5", "C2", "C3" etc.
 *
 * @param string The instrument string definition (name + frequency).
 * @return Display label with octave suffix, e.g. "G3".
 */
fun octaveLabel(string: InstrumentString): String {
    val midiNote = (69.0 + 12.0 * log2(string.frequency / 440.0)).roundToInt()
    val octave = (midiNote / 12) - 1
    return "${string.name}$octave"
}

@Composable
fun NoteTargetSelector(
    selectedNote: String?,
    isListening: Boolean,
    appLanguage: AppLanguage,
    onNoteSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    instrument: Instrument = Instrument.VIOLIN
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for (string in instrument.strings) {
            val note = string.name
            val displayLabel = octaveLabel(string)
            val freqLabel = "%.1f Hz".format(string.frequency)
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
                            onNoteSelected(null) // stop continuous
                        } else {
                            onNoteSelected(note)
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
                        text = displayLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = freqLabel,
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
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Button(
                onClick = { onNoteSelected(null) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(36.dp).testTag("stop_string_sound")
            ) {
                Text("\uD83D\uDD07", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(Localization.get("stop_synth_sound", appLanguage), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
