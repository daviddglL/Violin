package com.violinmaster.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.violinmaster.app.ui.screens.BorderStrokeHelper
import com.violinmaster.app.ui.theme.AppLanguage
import com.violinmaster.app.ui.theme.Localization

@Composable
fun PitchDisplay(
    selectedNote: String?,
    pitchOffsetCents: Float,
    isListening: Boolean,
    appLanguage: AppLanguage,
    needleAngleOffset: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
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
                TuningWheel(
                    needleAngleOffset = needleAngleOffset,
                    pitchOffsetCents = pitchOffsetCents,
                    isListening = isListening
                )

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
}
