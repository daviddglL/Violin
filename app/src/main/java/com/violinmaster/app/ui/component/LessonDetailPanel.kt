package com.violinmaster.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.violinmaster.app.data.LessonProgress
import com.violinmaster.app.di.SessionManager
import com.violinmaster.app.ui.screens.LessonDetailsContent
import com.violinmaster.app.ui.theme.AppLanguage
import com.violinmaster.app.ui.theme.Localization
import com.violinmaster.app.ui.viewmodel.PracticeViewModel

@Composable
fun LessonDetailPanel(
  extraDetails: LessonDetailsContent?,
  lesson: LessonProgress,
  practiceVM: PracticeViewModel,
  appLanguage: AppLanguage,
  sessionManager: SessionManager,
  isPracticing: Boolean,
  practiceCategory: String
) {
  Column(modifier = Modifier.padding(top = 16.dp)) {
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
    Spacer(modifier = Modifier.height(12.dp))

    if (extraDetails != null) {
      Text(
        text = extraDetails.description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = 18.sp
      )
      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = "🎯 " + Localization.get("stage_learning_objectives", appLanguage),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 0.5.sp
      )
      Spacer(modifier = Modifier.height(4.dp))
      extraDetails.objectives.forEach { obj ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
          verticalAlignment = Alignment.Top
        ) {
          Text(
            text = "✓",
            color = Color(0xFF81C784),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 6.dp)
          )
          Text(
            text = obj,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = "📝 " + Localization.get("included_core_drills", appLanguage),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.secondary,
        letterSpacing = 0.5.sp
      )
      Spacer(modifier = Modifier.height(4.dp))
      extraDetails.subtopics.forEach { sub ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "◽",
            modifier = Modifier.padding(end = 6.dp),
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = sub,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Button(
        onClick = { practiceVM.toggleLessonStatus(lesson.lessonId, !lesson.completed) },
        colors = ButtonDefaults.buttonColors(
          containerColor = if (lesson.completed) Color(0xFF2E7D32).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
          contentColor = if (lesson.completed) Color(0xFF81C784) else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag("check_lesson_${lesson.lessonId}")
      ) {
        Text(if (lesson.completed) "✓ " + Localization.get("lesson_completed_button", appLanguage) else Localization.get("mark_done_button", appLanguage))
      }

      val isCurrent = isPracticing && practiceCategory == lesson.lessonTitle
      Button(
        onClick = {
          practiceVM.startPracticeTimer(lesson.lessonTitle)
          sessionManager.selectTab(0)
        },
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = if (isCurrent) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
          contentColor = Color.White
        ),
        modifier = Modifier.testTag("practice_button_${lesson.lessonId}")
      ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(if (isCurrent) "▶ " + Localization.get("practicing_label", appLanguage) else Localization.get("drill_time_button", appLanguage))
      }
    }
  }
}
