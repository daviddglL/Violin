package com.violinmaster.app.ui.component

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.violinmaster.app.domain.model.Instrument
import com.violinmaster.app.ui.screens.BorderStrokeHelper
import com.violinmaster.app.ui.theme.AppLanguage
import com.violinmaster.app.ui.theme.Localization
import com.violinmaster.app.ui.viewmodel.TunerViewModel

data class FingeringNote(
  val finger: String,
  val noteName: String,
  val frequency: Double,
  val description: String
)

val fingeringMap = mapOf(
  "G" to listOf(
    FingeringNote("Open", "G3", 196.00, "Base note G of the violin wood, deep and resonant."),
    FingeringNote("1st Pos", "A3", 220.00, "Whole step from G open string. Active unison check."),
    FingeringNote("Low 2nd", "B♭3", 233.08, "Half step from 1st finger. Used in minor scales."),
    FingeringNote("High 2nd", "B3", 246.94, "Whole step from 1st finger. Major scale interval."),
    FingeringNote("3rd Pos", "C4", 261.63, "Half step from High 2nd finger. Forms octave check."),
    FingeringNote("4th Pos", "D4", 293.66, "Perfect unison double stop resonance with open D.")
  ),
  "D" to listOf(
    FingeringNote("Open", "D4", 293.66, "Warm central string, crucial for fundamental melodies."),
    FingeringNote("1st Pos", "E4", 329.63, "Whole step from D open. Anchors the D Major hand frame."),
    FingeringNote("Low 2nd", "F4", 349.23, "Half step from 1st finger, minor third resonance."),
    FingeringNote("High 2nd", "F♯4", 369.99, "Whole step from 1st finger. Standard D Major third."),
    FingeringNote("3rd Pos", "G4", 392.00, "Half step from High 2nd finger. Unison with G open."),
    FingeringNote("4th Pos", "A4", 440.00, "Unison resonance of absolute pitch with open A string.")
  ),
  "A" to listOf(
    FingeringNote("Open", "A4", 440.00, "The universal reference tuning pitch for orchestras."),
    FingeringNote("1st Pos", "B4", 493.88, "Whole step from A. Used extensively in first melodies."),
    FingeringNote("Low 2nd", "C5", 523.25, "Half step from 1st finger. Central C note in first-pos."),
    FingeringNote("High 2nd", "C♯5", 554.37, "Whole step from 1st finger. Major third in A Major."),
    FingeringNote("3rd Pos", "D5", 587.33, "Half step from High 2nd. Clean octave resonance check."),
    FingeringNote("4th Pos", "E5", 659.25, "Matches the open E pitch precisely. Tests pinky strength.")
  ),
  "E" to listOf(
    FingeringNote("Open", "E5", 659.25, "Bright, brilliant, projecting steel string pitch."),
    FingeringNote("1st Pos", "F♯5", 739.99, "Whole step from E. Requires soft high finger curve."),
    FingeringNote("Low 2nd", "G5", 783.99, "Half step from 1st. Brilliant, crisp minor third pitch."),
    FingeringNote("High 2nd", "G♯5", 830.61, "Whole step from 1st. High major third resonance."),
    FingeringNote("3rd Pos", "A5", 880.00, "One octave higher than open A. Check projection rings."),
    FingeringNote("4th Pos", "B5", 987.77, "Very high first-pos pitch. Requires soft, accurate touch.")
  )
)

@Composable
fun VirtualFingerboard(
  tunerVM: TunerViewModel,
  appLanguage: AppLanguage = AppLanguage.ENGLISH,
  instrument: Instrument = Instrument.VIOLIN
) {
  val instrumentStringNames = instrument.strings.map { it.name }.toSet()

  // Filter fingeringMap to only show strings belonging to the active instrument
  val supportedStrings = fingeringMap.keys.filter { it in instrumentStringNames }

  // Fall back to the instrument's first string if no overlap with fingering map
  val fallbackString = instrument.strings.firstOrNull()?.name ?: "A"
  val defaultString = supportedStrings.firstOrNull() ?: fallbackString
  var selectedFretString by remember { mutableStateOf(defaultString) }

  // Reset string tab selection when instrument changes
  LaunchedEffect(instrument) {
    selectedFretString = supportedStrings.firstOrNull() ?: fallbackString
  }

  val notesAndPositions = if (selectedFretString in instrumentStringNames) {
    fingeringMap[selectedFretString] ?: emptyList()
  } else {
    emptyList()
  }
  var activeFingeringNote by remember { mutableStateOf<FingeringNote?>(null) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
      .verticalScroll(rememberScrollState())
  ) {
    Text(
      text = Localization.get("select_current_string", appLanguage),
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.secondary,
      fontWeight = FontWeight.Bold,
      letterSpacing = 1.sp
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      instrument.strings.forEach { string ->
        val s = string.name
        val isSelected = selectedFretString == s
        Box(
          modifier = Modifier
            .weight(1f)
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF49454F), RoundedCornerShape(12.dp))
            .clickable {
              selectedFretString = s
              activeFingeringNote = null
            },
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = String.format(Localization.get("string_label_format", appLanguage), s),
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White,
            style = MaterialTheme.typography.titleMedium
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
      text = Localization.get("virtual_fingerboard", appLanguage),
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.secondary,
      fontWeight = FontWeight.Bold,
      letterSpacing = 1.sp
    )
    Spacer(modifier = Modifier.height(8.dp))

    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(BorderStrokeHelper(), RoundedCornerShape(16.dp)),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1005))
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 16.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        if (instrument != Instrument.VIOLIN) {
          // Placeholder for non-violin instruments
          Text(
            text = Localization.get("fingering_violin_only", appLanguage),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 24.dp)
          )
        } else {
          notesAndPositions.forEach { fNote ->
            val isFingerActive = activeFingeringNote?.finger == fNote.finger
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (isFingerActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent)
                .clickable {
                  activeFingeringNote = fNote
                  tunerVM.playCustomFrequency(fNote.frequency)
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(34.dp)
                    .background(
                      if (isFingerActive) MaterialTheme.colorScheme.primary else Color(0xFF493628),
                      CircleShape
                    ),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = fNote.finger.take(1),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 13.sp
                  )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Text(
                    text = fNote.finger,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                  )
                  Text(
                    text = Localization.get("position_relative_tape", appLanguage),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              Box(
                modifier = Modifier
                  .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                  .padding(horizontal = 10.dp, vertical = 4.dp)
              ) {
                Text(
                  text = String.format(Localization.get("note_frequency_format", appLanguage), fNote.noteName, fNote.frequency.toInt()),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.ExtraBold
                )
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    AnimatedVisibility(visible = activeFingeringNote != null) {
      val fn = activeFingeringNote
      if (fn != null) {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          border = BorderStrokeHelper()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column {
                Text(
                  text = String.format(Localization.get("string_target_format", appLanguage), selectedFretString, fn.noteName),
                  style = MaterialTheme.typography.titleMedium,
                  color = Color.White,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = String.format(Localization.get("frequency_match_format", appLanguage), fn.frequency),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.primary
                )
              }

              Button(
                onClick = { tunerVM.stopAudioEngineTone() },
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.error,
                  contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text("Mute Tone", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = fn.description,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                .padding(8.dp)
            ) {
              Text("💡", fontSize = 16.sp)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = Localization.get("listen_and_match", appLanguage),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(32.dp))
  }
}
