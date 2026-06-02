package com.violinmaster.app.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.violinmaster.app.di.AuthManager
import com.violinmaster.app.di.UserPreferencesManager
import com.violinmaster.app.ui.screens.ChatScreen
import com.violinmaster.app.ui.screens.VideoRecordScreen
import com.violinmaster.app.ui.theme.Localization
import com.violinmaster.app.ui.viewmodel.AssignmentViewModel
import com.violinmaster.app.ui.viewmodel.AuthViewModel
import com.violinmaster.app.ui.viewmodel.ChatViewModel
import com.violinmaster.app.ui.viewmodel.VideoUploadViewModel

@Composable
fun StudentAssignmentsTab(
  assignmentVM: AssignmentViewModel,
  authVM: AuthViewModel,
  userPreferencesManager: UserPreferencesManager,
  authManager: AuthManager,
  chatViewModel: ChatViewModel,
  videoViewModel: VideoUploadViewModel? = null,
  onPlayTutorialVideo: (String, String) -> Unit,
  modifier: Modifier = Modifier
) {
  val lang by userPreferencesManager.appLanguage.collectAsState()
  val student by authManager.currentUser.collectAsState()
  val assignments by assignmentVM.studentAssignments.collectAsState()

  var inputTeacherCode by remember { mutableStateOf("") }
  val linkedTeacher = student?.teacherCode ?: ""

  var selectedChatAssignment by remember { mutableStateOf<Pair<String, String>?>(null) }
  var isRecordingVideo by remember { mutableStateOf(false) }

  if (isRecordingVideo && videoViewModel != null) {
    VideoRecordScreen(
      viewModel = videoViewModel,
      lang = lang,
      onVideoSent = { videoUrl ->
        isRecordingVideo = false
        chatViewModel.sendVideoAttachment(videoUrl)
      },
      onCancel = {
        isRecordingVideo = false
        videoViewModel.cancelRecording()
      }
    )
    return
  }

  if (selectedChatAssignment != null) {
    val (assignmentId, assignmentTitle) = selectedChatAssignment!!
    LaunchedEffect(assignmentId) {
      chatViewModel.loadAssignment(assignmentId)
    }
    ChatScreen(
      chatViewModel = chatViewModel,
      assignmentTitle = assignmentTitle,
      lang = lang,
      onBack = { selectedChatAssignment = null }
    )
    return
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    if (linkedTeacher.isEmpty() || linkedTeacher.length < 3) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Box(
          modifier = Modifier
            .size(72.dp)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(36.dp)
          )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
          text = Localization.get("link_teacher_title", lang),
          style = MaterialTheme.typography.titleMedium,
          color = Color.White,
          fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = Localization.get("link_teacher_desc", lang),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 16.dp),
          lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
          value = inputTeacherCode,
          onValueChange = { inputTeacherCode = it.uppercase() },
          placeholder = { Text(Localization.get("input_code_hint", lang), fontSize = 12.sp) },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth(0.9f)
            .testTag("teacher_code_field")
        )

        Spacer(modifier = Modifier.height(18.dp))

        Button(
          onClick = {
            if (inputTeacherCode.isNotEmpty()) {
              authVM.linkTeacherCode(inputTeacherCode)
              inputTeacherCode = ""
            }
          },
          enabled = inputTeacherCode.isNotEmpty(),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth(0.9f)
        ) {
          Text(
            text = Localization.get("btn_link_teacher", lang).uppercase(),
            fontWeight = FontWeight.Bold
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(12.dp)
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .background(MaterialTheme.colorScheme.secondary, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSecondary,
                  modifier = Modifier.size(18.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = Localization.get("linked_to_teacher", lang),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.secondary,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = linkedTeacher,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                  fontSize = 15.sp
                )
              }
            }
          }
        }

        item {
          Text(
            text = Localization.get("assignments_section_title", lang),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
        }

        if (assignments.isEmpty()) {
          item {
            Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              shape = RoundedCornerShape(14.dp)
            ) {
              Text(
                text = Localization.get("no_assignments", lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
              )
            }
          }
        }

        items(assignments) { task ->
          val isDone = task.completed
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
              containerColor = if (isDone) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
              1.dp,
              if (isDone) Color.Green.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f)
            )
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                      if (isDone) Color.Green.copy(alpha = 0.15f)
                      else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                  Text(
                    text = if (isDone) Localization.get("completed_tag", lang) else Localization.get("pending_label", lang),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDone) Color.Green else MaterialTheme.colorScheme.primary
                  )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                  IconButton(
                    onClick = {
                      selectedChatAssignment = Pair(task.id.toString(), task.title)
                    },
                    modifier = Modifier.size(28.dp)
                  ) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.Chat,
                      contentDescription = Localization.get("chat_button", lang),
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(16.dp)
                    )
                  }

                  if (videoViewModel != null) {
                    IconButton(
                      onClick = {
                        chatViewModel.loadAssignment(task.id.toString())
                        isRecordingVideo = true
                      },
                      modifier = Modifier.size(28.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Filled.Videocam,
                        contentDescription = Localization.get("video_record_button", lang),
                        tint = Color(0xFFFF5722),
                        modifier = Modifier.size(16.dp)
                      )
                    }
                  }

                  Text(
                    text = Localization.get("btn_complete", lang),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                      assignmentVM.markAssignmentComplete(task.id, !isDone)
                    }
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Checkbox(
                    checked = isDone,
                    onCheckedChange = { assignmentVM.markAssignmentComplete(task.id, it) }
                  )
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDone) Color.White.copy(alpha = 0.6f) else Color.White
              )

              Spacer(modifier = Modifier.height(6.dp))

              Text(
                text = task.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp,
                fontSize = 11.5.sp
              )

              if (task.videoTitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "⏱ " + String.format(Localization.get("video_demo_format", lang), task.videoDurationSeconds),
                      style = MaterialTheme.typography.bodySmall,
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                      text = task.videoTitle,
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.primary,
                      fontSize = 11.sp
                    )
                  }

                  Button(
                    onClick = { onPlayTutorialVideo(task.videoResourceUrl, task.videoTitle) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp)
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                      Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                      Text(Localization.get("play_sim_video", lang), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
