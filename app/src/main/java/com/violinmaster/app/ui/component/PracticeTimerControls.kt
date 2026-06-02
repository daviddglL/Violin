package com.violinmaster.app.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.violinmaster.app.ui.theme.AppLanguage
import com.violinmaster.app.ui.theme.Localization
import com.violinmaster.app.ui.viewmodel.PracticeViewModel
import java.util.Locale

@Composable
fun PracticeTimerControls(
  practiceVM: PracticeViewModel,
  practiceCategory: String,
  practiceElapsed: Int,
  appLanguage: AppLanguage,
  visible: Boolean = true,
  modifier: Modifier = Modifier
) {
  AnimatedVisibility(visible = visible) {
    Card(
      modifier = modifier
        .fillMaxWidth()
        .testTag("active_timer_card"),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer
      ),
      elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = Localization.get("active_practice_label", appLanguage),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = practiceCategory,
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))

        val mins = practiceElapsed / 60
        val secs = practiceElapsed % 60
        val timerDisplay = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

        Text(
          text = timerDisplay,
          style = MaterialTheme.typography.displayLarge,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          fontWeight = FontWeight.Medium,
          fontSize = 48.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Button(
            onClick = { practiceVM.stopAndSavePracticeSession() },
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
              contentColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("save_practice_button")
          ) {
            Icon(Icons.Default.Check, contentDescription = Localization.get("save_practice_cd", appLanguage))
            Spacer(modifier = Modifier.width(6.dp))
            Text(Localization.get("save_and_log_button", appLanguage), fontWeight = FontWeight.Bold)
          }

          Spacer(modifier = Modifier.width(12.dp))

          Button(
            onClick = { practiceVM.cancelPracticeTimer() },
            colors = ButtonDefaults.buttonColors(
              containerColor = Color(0xFFC53030),
              contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .height(48.dp)
              .testTag("cancel_practice_button")
          ) {
            Icon(Icons.Default.Refresh, contentDescription = Localization.get("cancel_practice_cd", appLanguage))
            Spacer(modifier = Modifier.width(4.dp))
            Text(Localization.get("discard_button", appLanguage))
          }
        }
      }
    }
  }
}
