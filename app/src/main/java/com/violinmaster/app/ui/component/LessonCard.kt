package com.violinmaster.app.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.violinmaster.app.ui.screens.BorderStrokeHelper
import com.violinmaster.app.ui.theme.AppLanguage
import com.violinmaster.app.ui.theme.Localization
import com.violinmaster.app.ui.viewmodel.PracticeViewModel

@Composable
fun LessonCard(
  lesson: LessonProgress,
  difficulty: String,
  isExpanded: Boolean,
  onToggleExpand: () -> Unit,
  practiceVM: PracticeViewModel,
  appLanguage: AppLanguage,
  navigationManager: com.violinmaster.app.di.NavigationManager,
  isPracticing: Boolean,
  practiceCategory: String
) {
  val extraDetails = lessonDetailsMap[lesson.lessonId]

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("lesson_item_${lesson.lessonId}")
      .clickable(enabled = true, onClick = onToggleExpand),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    border = if (isExpanded) {
      BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
      BorderStrokeHelper()
    }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (lesson.completed) "✅" else "🎻",
          fontSize = 20.sp,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = lesson.lessonTitle,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
          )
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = String.format(Localization.get("level_format", appLanguage), difficulty) + " \u2022 ",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = String.format(Localization.get("minutes_practiced_format", appLanguage), lesson.totalPracticedSeconds / 60),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold
            )
          }
        }
        Text(
          text = if (isExpanded) "▲" else "▼",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.secondary
        )
      }

      if (isExpanded) {
        LessonDetailPanel(
          extraDetails = extraDetails,
          lesson = lesson,
          practiceVM = practiceVM,
          appLanguage = appLanguage,
          navigationManager = navigationManager,
          isPracticing = isPracticing,
          practiceCategory = practiceCategory
        )
      }
    }
  }
}
