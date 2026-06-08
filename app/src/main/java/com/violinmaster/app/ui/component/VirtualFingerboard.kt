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
  // ── VIOLIN ─────────────────────────────────────────────────────
  Instrument.VIOLIN to mapOf(
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
  ),

  // ── VIOLA ──────────────────────────────────────────────────────
  Instrument.VIOLA to mapOf(
    "C" to listOf(
      FingeringNote("Open", "C3", 130.81, "Fundamental C of the viola, warm and rich alto voice."),
      FingeringNote("1st Pos", "D3", 146.83, "Whole step from C open. Anchors the viola hand frame."),
      FingeringNote("Low 2nd", "E♭3", 155.56, "Half step from 1st finger. Minor-mode tonal color."),
      FingeringNote("High 2nd", "E3", 164.81, "Whole step from 1st finger. Major third resonance."),
      FingeringNote("3rd Pos", "F3", 174.61, "Half step from High 2nd. Perfect fourth from open."),
      FingeringNote("4th Pos", "G3", 196.00, "Perfect fifth from C open, unison with open G string.")
    ),
    "G" to listOf(
      FingeringNote("Open", "G3", 196.00, "Warm viola G, sonorous alto register anchor."),
      FingeringNote("1st Pos", "A3", 220.00, "Whole step from G open. First-position hand reference."),
      FingeringNote("Low 2nd", "B♭3", 233.08, "Half step from 1st finger. Minor scale interval."),
      FingeringNote("High 2nd", "B3", 246.94, "Whole step from 1st finger. Major scale interval."),
      FingeringNote("3rd Pos", "C4", 261.63, "Half step from High 2nd. Middle C, octave check."),
      FingeringNote("4th Pos", "D4", 293.66, "Perfect unison resonance with open D string.")
    ),
    "D" to listOf(
      FingeringNote("Open", "D4", 293.66, "Viola D — central midrange, lyrical and expressive."),
      FingeringNote("1st Pos", "E4", 329.63, "Whole step from D open. Hand-frame anchor."),
      FingeringNote("Low 2nd", "F4", 349.23, "Half step from 1st finger. Minor third color."),
      FingeringNote("High 2nd", "F♯4", 369.99, "Whole step from 1st. D Major third interval."),
      FingeringNote("3rd Pos", "G4", 392.00, "Half step from High 2nd. Octave above open G."),
      FingeringNote("4th Pos", "A4", 440.00, "Perfect unison with open A, absolute pitch reference.")
    ),
    "A" to listOf(
      FingeringNote("Open", "A4", 440.00, "Viola A — bright upper register, orchestra tuning reference."),
      FingeringNote("1st Pos", "B4", 493.88, "Whole step from A open. First-position top note."),
      FingeringNote("Low 2nd", "C5", 523.25, "Half step from 1st. High C in first position."),
      FingeringNote("High 2nd", "C♯5", 554.37, "Whole step from 1st. High major third."),
      FingeringNote("3rd Pos", "D5", 587.33, "Half step from High 2nd. Clean octave above open D."),
      FingeringNote("4th Pos", "E5", 659.25, "Top of first-position range. Tests fourth-finger strength.")
    )
  ),

  // ── CELLO ──────────────────────────────────────────────────────
  Instrument.CELLO to mapOf(
    "C" to listOf(
      FingeringNote("Open", "C2", 65.41, "Deepest cello string, resonant and foundational bass voice."),
      FingeringNote("1st Pos", "D2", 73.42, "Whole step from C open. First-position anchor."),
      FingeringNote("Low 2nd", "E♭2", 77.78, "Half step from 1st finger. Minor-mode depth."),
      FingeringNote("High 2nd", "E2", 82.41, "Whole step from 1st finger. Major third resonance."),
      FingeringNote("3rd Pos", "F2", 87.31, "Half step from High 2nd. Perfect fourth from C."),
      FingeringNote("4th Pos", "G2", 98.00, "Perfect fifth, unison with open G string.")
    ),
    "G" to listOf(
      FingeringNote("Open", "G2", 98.00, "Rich cello G — warm mid-bass, foundational for melody."),
      FingeringNote("1st Pos", "A2", 110.00, "Whole step from G open. Hand-frame reference."),
      FingeringNote("Low 2nd", "B♭2", 116.54, "Half step from 1st. Minor third interval."),
      FingeringNote("High 2nd", "B2", 123.47, "Whole step from 1st. Major third in G Major."),
      FingeringNote("3rd Pos", "C3", 130.81, "Half step from High 2nd. Octave above open C."),
      FingeringNote("4th Pos", "D3", 146.83, "Perfect unison with open D string.")
    ),
    "D" to listOf(
      FingeringNote("Open", "D3", 146.83, "Cello D — midrange warmth, lyrical tenor voice."),
      FingeringNote("1st Pos", "E3", 164.81, "Whole step from D open. Hand-frame anchor."),
      FingeringNote("Low 2nd", "F3", 174.61, "Half step from 1st. Minor third color."),
      FingeringNote("High 2nd", "F♯3", 185.00, "Whole step from 1st. D Major third."),
      FingeringNote("3rd Pos", "G3", 196.00, "Half step from High 2nd. Octave above open G."),
      FingeringNote("4th Pos", "A3", 220.00, "Perfect unison with open A string.")
    ),
    "A" to listOf(
      FingeringNote("Open", "A3", 220.00, "Cello A — penetrating upper tenor, solo voice."),
      FingeringNote("1st Pos", "B3", 246.94, "Whole step from A open. Top of first position."),
      FingeringNote("Low 2nd", "C4", 261.63, "Half step from 1st. Middle C — landmark pitch."),
      FingeringNote("High 2nd", "C♯4", 277.18, "Whole step from 1st. Major third above A."),
      FingeringNote("3rd Pos", "D4", 293.66, "Half step from High 2nd. Octave above open D."),
      FingeringNote("4th Pos", "E4", 329.63, "Top of cello first-position range.")
    )
  ),

  // ── DOUBLE BASS ────────────────────────────────────────────────
  Instrument.DOUBLE_BASS to mapOf(
    "E" to listOf(
      FingeringNote("Open", "E1", 41.20, "Lowest orchestral string, deep fundamental earth pitch."),
      FingeringNote("1st Pos", "F♯1", 46.25, "Whole step from E open. First-position anchor."),
      FingeringNote("Low 2nd", "G1", 49.00, "Half step from 1st finger. Minor third depth."),
      FingeringNote("High 2nd", "G♯1", 51.91, "Whole step from 1st finger. Major third interval."),
      FingeringNote("3rd Pos", "A1", 55.00, "Half step from High 2nd. Perfect fourth, unison with open A."),
      FingeringNote("4th Pos", "B1", 61.74, "Perfect fifth from E open. Extends the E-string frame.")
    ),
    "A" to listOf(
      FingeringNote("Open", "A1", 55.00, "Bass A — resonant low foundation, orchestral anchor."),
      FingeringNote("1st Pos", "B1", 61.74, "Whole step from A open. Hand-frame reference."),
      FingeringNote("Low 2nd", "C2", 65.41, "Half step from 1st. Low C landmark pitch."),
      FingeringNote("High 2nd", "C♯2", 69.30, "Whole step from 1st. Major third from open A."),
      FingeringNote("3rd Pos", "D2", 73.42, "Half step from High 2nd. Unison with open D."),
      FingeringNote("4th Pos", "E2", 82.41, "Perfect fifth from A open, octave above open E.")
    ),
    "D" to listOf(
      FingeringNote("Open", "D2", 73.42, "Bass D — warm mid-bass, melodic centre of gravity."),
      FingeringNote("1st Pos", "E2", 82.41, "Whole step from D open. Hand-frame anchor."),
      FingeringNote("Low 2nd", "F2", 87.31, "Half step from 1st. Minor third color."),
      FingeringNote("High 2nd", "F♯2", 92.50, "Whole step from 1st. D Major third."),
      FingeringNote("3rd Pos", "G2", 98.00, "Half step from High 2nd. Unison with open G."),
      FingeringNote("4th Pos", "A2", 110.00, "Perfect fifth from D open, octave above open A.")
    ),
    "G" to listOf(
      FingeringNote("Open", "G2", 98.00, "Bass G — highest bass string, singing upper register."),
      FingeringNote("1st Pos", "A2", 110.00, "Whole step from G open. Top of first-position hand."),
      FingeringNote("Low 2nd", "B♭2", 116.54, "Half step from 1st. Minor-mode color."),
      FingeringNote("High 2nd", "B2", 123.47, "Whole step from 1st. G Major third."),
      FingeringNote("3rd Pos", "C3", 130.81, "Half step from High 2nd. Octave above open C."),
      FingeringNote("4th Pos", "D3", 146.83, "Perfect fifth from open G, top of double-bass range.")
    )
  )
)

@Composable
fun VirtualFingerboard(
  tunerVM: TunerViewModel,
  appLanguage: AppLanguage = AppLanguage.ENGLISH,
  instrument: Instrument = Instrument.VIOLIN
) {
  val instrumentStringNames = instrument.strings.map { it.name }.toSet()

  // Access the per-instrument fingering sub-map
  val instrumentFingering = fingeringMap[instrument] ?: emptyMap()

  // Show strings that exist in both the instrument definition AND fingering data
  val supportedStrings = instrumentFingering.keys.filter { it in instrumentStringNames }

  // Fall back to the instrument's first string if no overlap
  val fallbackString = instrument.strings.firstOrNull()?.name ?: "A"
  val defaultString = supportedStrings.firstOrNull() ?: fallbackString
  var selectedFretString by remember { mutableStateOf(defaultString) }

  // Reset string tab selection when instrument changes
  LaunchedEffect(instrument) {
    selectedFretString = supportedStrings.firstOrNull() ?: fallbackString
  }

  val notesAndPositions = instrumentFingering[selectedFretString] ?: emptyList()
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
