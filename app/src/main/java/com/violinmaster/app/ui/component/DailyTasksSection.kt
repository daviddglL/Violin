package com.violinmaster.app.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.violinmaster.app.ui.theme.AppLanguage
import com.violinmaster.app.ui.theme.Localization
import com.violinmaster.app.ui.viewmodel.PracticeViewModel

data class DailyTaskItem(
  val id: String,
  val titleEn: String,
  val titleEs: String,
  val descEn: String,
  val descEs: String,
  val category: String
)

val beginnerDailyTasks = listOf(
  DailyTaskItem("beg_dt1", "Open Resonance Bowing", "Alineación y Arcos de Cuerda al Aire", "Practice full whole bows on open strings D and A.", "Practica arcos enteros en cuerdas libres Re y La.", "Open Strings Tuning & Bowing"),
  DailyTaskItem("beg_dt2", "Bow Hold Pinky Taps", "Toques de Meñique en el Arco", "Perform 15 clean pinky taps to build finger flexibility.", "Realiza 15 toques de meñique para ganar flexibilidad.", "Posture Check & Bow Grip"),
  DailyTaskItem("beg_dt3", "Precision Ear Tuner", "Afinación Auditiva de Precisión", "Match 3 strings to the smart tuner perfect pitches.", "Sincroniza 3 cuerdas al aire con el afinador inteligente.", "Smart Tuner Tuning")
)

val intermediateDailyTasks = listOf(
  DailyTaskItem("int_dt1", "Smooth Shifting Slide", "Deslizamiento Fa# en 3ª Posición", "Glide finger 2 up to third position and verify pitch.", "Desliza el dedo 2 a la tercera posición y analiza el tono.", "Shifting to Third Position (III)"),
  DailyTaskItem("int_dt2", "Warm Pulsed Vibrato", "Vibrato de Calor Pulsado", "Oscillate fingers on the G string for 1 minute.", "Oscila los dedos en cuerda Sol con vibrato por 1 minuto.", "Relaxing Left Hand & Vibrato"),
  DailyTaskItem("int_dt3", "Double Stop Stability", "Estabilidad en Doble Cuerda", "Play fourths & fifths balancing bow hair weight.", "Toca cuartas y quintas equilibrando el peso del arco.", "Double Stop Balance & Harmony")
)

val advancedDailyTasks = listOf(
  DailyTaskItem("adv_dt1", "Gravity Spiccato Nodes", "Rebotes de Madera Spiccato", "Bounce the bow rapid sixteenths at 110 BPM.", "Rebota el arco en velocidad de semicorcheas a 110 BPM.", "Bowing Styles: Martelé, Spiccato"),
  DailyTaskItem("adv_dt2", "Violin Neck Extreme Shift", "Cambio Extremo de Diapasón", "Perform shifts to 5th position on the A string.", "Realiza cambios a la quinta posición en la cuerda La.", "High Position Shifts (5th & 7th)"),
  DailyTaskItem("adv_dt3", "Paganini Velocity Run", "Arpegio del Diablo Paganini", "Coordinate rapid string crossings cleanly.", "Coordina arpegios rápidos en cruzado de cuerdas limpio.", "Paganini Practice Theme (A minor)")
)

@Composable
fun DailyTasksSection(
  practiceVM: PracticeViewModel,
  appLanguage: AppLanguage,
  skillLevel: String,
  dailyTasksCompleted: Set<String>,
  modifier: Modifier = Modifier
) {
  val dailyTasksList = when (skillLevel) {
    "Intermediate" -> intermediateDailyTasks
    "Advanced" -> advancedDailyTasks
    else -> beginnerDailyTasks
  }

  var activeTaskForCompletion by remember { mutableStateOf<DailyTaskItem?>(null) }
  var selectedAttemptsCount by remember { mutableStateOf(1) }

  Column(modifier = modifier) {
    Text(
      text = Localization.get("daily_tasks_title", appLanguage),
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.secondary,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(start = 4.dp, top = 4.dp),
      letterSpacing = 1.5.sp
    )

    dailyTasksList.forEach { task ->
      val isCompleted = dailyTasksCompleted.contains(task.id)
      val titleText = if (appLanguage == AppLanguage.SPANISH) task.titleEs else task.titleEn
      val descText = if (appLanguage == AppLanguage.SPANISH) task.descEs else task.descEn

      Card(
        modifier = Modifier.fillMaxWidth()
          .testTag("daily_task_item_${task.id}"),
        colors = CardDefaults.cardColors(
          containerColor = if (isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
          1.dp,
          if (isCompleted) Color(0xFF81C784).copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant
        )
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              if (isCompleted) {
                Text("✅", fontSize = 16.sp, modifier = Modifier.padding(end = 6.dp))
              } else {
                Text("🎯", fontSize = 16.sp, modifier = Modifier.padding(end = 6.dp))
              }
              Text(
                text = titleText,
                style = MaterialTheme.typography.titleSmall,
                color = if (isCompleted) Color(0xFF81C784) else Color.White,
                fontWeight = FontWeight.Bold
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = descText,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              lineHeight = 16.sp
            )
          }
          Spacer(modifier = Modifier.width(12.dp))

          if (isCompleted) {
            Surface(
              color = Color(0xFF81C784).copy(alpha = 0.15f),
              contentColor = Color(0xFF81C784),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text(
                text = Localization.get("completed_btn", appLanguage),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
              )
            }
          } else {
            Column(horizontalAlignment = Alignment.End) {
              Button(
                onClick = {
                  practiceVM.startPracticeTimer(task.category)
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primaryContainer,
                  contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
              ) {
                Text(
                  text = Localization.get("start_task", appLanguage),
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Button(
                onClick = {
                  activeTaskForCompletion = task
                  selectedAttemptsCount = 1
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.secondaryContainer,
                  contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(28.dp)
                  .testTag("complete_task_${task.id}")
              ) {
                Text(
                  text = "✓",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      }
    }
  }

  val taskToComplete = activeTaskForCompletion
  if (taskToComplete != null) {
    androidx.compose.material3.AlertDialog(
      onDismissRequest = { activeTaskForCompletion = null },
      title = {
        Text(
          text = Localization.get("attempts_needed_title", appLanguage),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = Localization.get("attempts_needed_subtitle", appLanguage),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(4.dp))

          listOf(1, 2, 3, 4).forEach { attemptOption ->
            val label = when (attemptOption) {
              1 -> Localization.get("attempt_1", appLanguage)
              2 -> Localization.get("attempt_2", appLanguage)
              3 -> Localization.get("attempt_3", appLanguage)
              else -> Localization.get("attempt_4", appLanguage)
            }
            val isSelected = selectedAttemptsCount == attemptOption
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedAttemptsCount = attemptOption }
                .clip(RoundedCornerShape(8.dp)),
              color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
              border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                androidx.compose.material3.RadioButton(
                  selected = isSelected,
                  onClick = { selectedAttemptsCount = attemptOption }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = label,
                  style = MaterialTheme.typography.bodyMedium,
                  color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            practiceVM.completeDailyTask(taskToComplete.id, selectedAttemptsCount)
            activeTaskForCompletion = null
          }
        ) {
          Text(
            text = Localization.get("confirm_completion", appLanguage),
            fontWeight = FontWeight.Bold
          )
        }
      },
      dismissButton = {
        androidx.compose.material3.TextButton(
          onClick = { activeTaskForCompletion = null }
        ) {
          Text(text = Localization.get("cancel_button", appLanguage))
        }
      }
    )
  }
}
