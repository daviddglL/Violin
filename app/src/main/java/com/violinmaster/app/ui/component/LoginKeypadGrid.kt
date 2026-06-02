package com.violinmaster.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.violinmaster.app.ui.theme.AppLanguage

@Composable
fun LoginKeypadGrid(
  currentValue: String,
  onValueChange: (String) -> Unit,
  onValidate: () -> Unit,
  lang: AppLanguage,
  isCorrectLength: Boolean
) {
  val keys = listOf(
    "1", "2", "3",
    "4", "5", "6",
    "7", "8", "9",
    "CLR", "0", "OK"
  )

  Column(
    modifier = Modifier.width(320.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    val rows = keys.chunked(3)
    rows.forEach { rowKeys ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        rowKeys.forEach { key ->
          val isAction = key == "CLR" || key == "OK"
          val isOkActive = key == "OK" && isCorrectLength

          Box(
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(
                when {
                  isOkActive -> MaterialTheme.colorScheme.primary
                  isAction -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                  else -> MaterialTheme.colorScheme.surface
                }
              )
              .clickable {
                when (key) {
                  "CLR" -> onValueChange("")
                  "OK" -> onValidate()
                  else -> {
                    if (currentValue.length < 4) {
                      onValueChange(currentValue + key)
                    }
                  }
                }
              }
              .testTag("login_keypad_$key"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = key,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              color = when {
                isOkActive -> MaterialTheme.colorScheme.onPrimary
                key == "CLR" -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> Color.White
              }
            )
          }
        }
      }
    }
  }
}
