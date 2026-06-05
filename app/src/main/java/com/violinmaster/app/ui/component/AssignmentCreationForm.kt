package com.violinmaster.app.ui.component

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.ui.theme.AppLanguage
import com.violinmaster.app.ui.theme.Localization
import com.violinmaster.app.ui.viewmodel.AssignmentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentCreationForm(
    lang: AppLanguage,
    linkedStudents: List<UserAccount>,
    assignmentVM: AssignmentViewModel,
    modifier: Modifier = Modifier
) {
    var assignTitle by remember { mutableStateOf("") }
    var assignDesc by remember { mutableStateOf("") }
    var selectedStudentUsername by remember { mutableStateOf("ALL") }

    var attachVideo by remember { mutableStateOf(false) }
    var videoTitle by remember { mutableStateOf("") }
    var videoDurationSeconds by remember { mutableStateOf(120) }
    var studentDropdownExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = Localization.get("send_assignment", lang).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = Localization.get("select_student", lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )

                Box {
                    Button(
                        onClick = { studentDropdownExpanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = if (selectedStudentUsername == "ALL") Localization.get("all_students", lang) else selectedStudentUsername,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                    }

                    DropdownMenu(
                        expanded = studentDropdownExpanded,
                        onDismissRequest = { studentDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(Localization.get("all_students", lang)) },
                            onClick = {
                                selectedStudentUsername = "ALL"
                                studentDropdownExpanded = false
                            }
                        )
                        linkedStudents.forEach { student ->
                            DropdownMenuItem(
                                text = { Text(student.username) },
                                onClick = {
                                    selectedStudentUsername = student.username
                                    studentDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Text(
                text = Localization.get("predefined_templates", lang),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val templates = listOf(
                    "temp_posture" to "temp_posture",
                    "temp_res" to "temp_res",
                    "temp_bounce" to "temp_bounce"
                )
                templates.forEach { (transKey, valueKey) ->
                    val textLabel = Localization.get(transKey, lang)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .clickable {
                                assignTitle = textLabel
                                assignDesc = when (valueKey) {
                                    "temp_posture" -> "Verify shoulder rest alignment. Back bone straight. Practice holding the instrument for 3 intervals of 60 seconds with no hand assistance."
                                    "temp_res" -> "Perform slow bowings on the G, D, and A strings. Isolate the perfect dynamic overtones on the resonant G octave."
                                    else -> "Bounce bow middle area repeatedly to achieve elastic spiccato balance. Target 8 repeated notes per single pulse."
                                }
                            }
                            .padding(8.dp)
                            .height(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = textLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 8.5.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 11.sp
                        )
                    }
                }
            }

            OutlinedTextField(
                value = assignTitle,
                onValueChange = { assignTitle = it },
                placeholder = { Text(Localization.get("assignment_title_hint", lang), fontSize = 12.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                ),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = assignDesc,
                onValueChange = { assignDesc = it },
                placeholder = { Text(Localization.get("assignment_desc_hint", lang), fontSize = 12.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(100.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = attachVideo,
                        onCheckedChange = { attachVideo = it }
                    )
                    Text(
                        text = Localization.get("attachment_allowed", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }

                if (attachVideo) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (attachVideo) {
                OutlinedTextField(
                    value = videoTitle,
                    onValueChange = { videoTitle = it },
                    placeholder = { Text(Localization.get("video_title_placeholder", lang), fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "${Localization.get("duration_sec_label", lang)} $videoDurationSeconds s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = videoDurationSeconds.toFloat(),
                        onValueChange = { videoDurationSeconds = it.toInt() },
                        valueRange = 10f..240f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    val exceedsLimit = videoDurationSeconds > 180
                    if (exceedsLimit) {
                        Text(
                            text = Localization.get("video_time_limit", lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            val canSubmit = assignTitle.isNotEmpty() && (!attachVideo || (attachVideo && videoDurationSeconds <= 180 && videoTitle.isNotEmpty()))

            Button(
                onClick = {
                    assignmentVM.publishAssignment(
                        title = assignTitle,
                        description = assignDesc,
                        targetStudent = selectedStudentUsername,
                        videoTitle = if (attachVideo) videoTitle else "",
                        durationSeconds = if (attachVideo) videoDurationSeconds else 0
                    )
                    assignTitle = ""
                    assignDesc = ""
                    videoTitle = ""
                    attachVideo = false
                },
                enabled = canSubmit,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = Localization.get("btn_submit_assignment", lang).uppercase(),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
