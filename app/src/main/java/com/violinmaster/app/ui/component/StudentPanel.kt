package com.violinmaster.app.ui.component

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
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
            TeacherCodeLinkingSection(
                lang = lang,
                inputTeacherCode = inputTeacherCode,
                onTeacherCodeChange = { inputTeacherCode = it.uppercase() },
                onLinkTeacher = {
                    if (inputTeacherCode.isNotEmpty()) {
                        authVM.linkTeacherCode(inputTeacherCode)
                        inputTeacherCode = ""
                    }
                }
            )
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
                    StudentAssignmentItemCard(
                        task = task,
                        lang = lang,
                        chatViewModel = chatViewModel,
                        videoViewModel = videoViewModel,
                        assignmentVM = assignmentVM,
                        onPlayTutorialVideo = onPlayTutorialVideo,
                        onStartChat = { id, title -> selectedChatAssignment = Pair(id, title) },
                        onStartRecording = { id ->
                            chatViewModel.loadAssignment(id)
                            isRecordingVideo = true
                        }
                    )
                }
            }
        }
    }
}
