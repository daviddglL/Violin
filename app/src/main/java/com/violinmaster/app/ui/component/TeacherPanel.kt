package com.violinmaster.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.violinmaster.app.di.AuthManager
import com.violinmaster.app.di.UserPreferencesManager
import com.violinmaster.app.ui.screens.ChatScreen
import com.violinmaster.app.ui.screens.VideoRecordScreen
import com.violinmaster.app.ui.viewmodel.AssignmentViewModel
import com.violinmaster.app.ui.viewmodel.ChatViewModel
import com.violinmaster.app.ui.viewmodel.VideoUploadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardTab(
    assignmentVM: AssignmentViewModel,
    userPreferencesManager: UserPreferencesManager,
    authManager: AuthManager,
    chatViewModel: ChatViewModel,
    videoViewModel: VideoUploadViewModel,
    modifier: Modifier = Modifier
) {
    val lang by userPreferencesManager.appLanguage.collectAsState()
    val teacher by authManager.currentUser.collectAsState()
    val allUsers by assignmentVM.allUsers.collectAsState()
    val sentAssignments by assignmentVM.teacherAssignments.collectAsState()

    var selectedChatAssignment by remember { mutableStateOf<Pair<String, String>?>(null) }
    var isRecordingVideo by remember { mutableStateOf(false) }

    val teacherCode = teacher?.teacherCode ?: "TEACH-0000"
    val linkedStudents = allUsers.filter { it.role == "STUDENT" && it.teacherCode == teacherCode }

    if (isRecordingVideo) {
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TeacherCodeSection(
                lang = lang,
                teacherUsername = teacher?.username ?: "",
                teacherCode = teacherCode
            )
        }

        item {
            AssignmentCreationForm(
                lang = lang,
                linkedStudents = linkedStudents,
                assignmentVM = assignmentVM
            )
        }

        item {
            SentAssignmentsList(
                lang = lang,
                sentAssignments = sentAssignments,
                assignmentVM = assignmentVM,
                videoViewModel = videoViewModel,
                onStartChat = { id, title -> selectedChatAssignment = Pair(id, title) },
                onStartRecording = { id ->
                    chatViewModel.loadAssignment(id)
                    isRecordingVideo = true
                }
            )
        }
    }
}
