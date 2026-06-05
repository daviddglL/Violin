package com.violinmaster.app.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.Chat
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.violinmaster.app.data.Assignment
import com.violinmaster.app.ui.theme.AppLanguage
import com.violinmaster.app.ui.theme.Localization
import com.violinmaster.app.ui.viewmodel.AssignmentViewModel
import com.violinmaster.app.ui.viewmodel.ChatViewModel
import com.violinmaster.app.ui.viewmodel.VideoUploadViewModel

@Composable
fun StudentAssignmentItemCard(
    task: Assignment,
    lang: AppLanguage,
    chatViewModel: ChatViewModel,
    videoViewModel: VideoUploadViewModel?,
    assignmentVM: AssignmentViewModel,
    onPlayTutorialVideo: (String, String) -> Unit,
    onStartChat: (assignmentId: String, title: String) -> Unit,
    onStartRecording: (assignmentId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDone = task.completed
    Card(
        modifier = modifier.fillMaxWidth(),
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
                            onStartChat(task.id.toString(), task.title)
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
                                onStartRecording(task.id.toString())
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
                            text = "\u23F1 " + String.format(Localization.get("video_demo_format", lang), task.videoDurationSeconds),
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
