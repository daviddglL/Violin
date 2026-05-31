package com.violinmaster.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.violinmaster.app.data.Assignment
import com.violinmaster.app.ui.theme.AppLanguage
import com.violinmaster.app.ui.theme.Localization
import com.violinmaster.app.data.firebase.Message
import com.violinmaster.app.ui.viewmodel.AssignmentViewModel
import com.violinmaster.app.ui.viewmodel.AuthViewModel
import com.violinmaster.app.ui.viewmodel.ChatViewModel
import com.violinmaster.app.ui.viewmodel.VideoUploadViewModel
import com.violinmaster.app.di.SessionManager

// ----------------------------------------------------
// TEACHER WORKSPACE PANEL
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardTab(
    assignmentVM: AssignmentViewModel,
    sessionManager: SessionManager,
    chatViewModel: ChatViewModel,
    videoViewModel: VideoUploadViewModel? = null,
    modifier: Modifier = Modifier
) {
    val lang by sessionManager.appLanguage.collectAsState()
    val teacher by sessionManager.currentUser.collectAsState()
    val allUsers by assignmentVM.allUsers.collectAsState()
    val sentAssignments by assignmentVM.teacherAssignments.collectAsState()

    var assignTitle by remember { mutableStateOf("") }
    var assignDesc by remember { mutableStateOf("") }
    var selectedStudentUsername by remember { mutableStateOf("ALL") } // "ALL" or student username
    
    var attachVideo by remember { mutableStateOf(false) }
    var videoTitle by remember { mutableStateOf("") }
    var videoDurationSeconds by remember { mutableStateOf(120) } // Default 2 minutes
    var studentDropdownExpanded by remember { mutableStateOf(false) }

    // Chat overlay state: Pair(assignmentId, assignmentTitle)
    var selectedChatAssignment by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Video recording overlay state
    var isRecordingVideo by remember { mutableStateOf(false) }

    val teacherCode = teacher?.teacherCode ?: "TEACH-0000"

    // Only get students linked to this teacher code
    val linkedStudents = allUsers.filter { it.role == "STUDENT" && it.teacherCode == teacherCode }

    // ── Video recording overlay ──────────────────────────────────────
    if (isRecordingVideo && videoViewModel != null) {
        VideoRecordScreen(
            viewModel = videoViewModel,
            lang = lang,
            onVideoSent = { videoUrl ->
                isRecordingVideo = false
                // Send video as chat message to current assignment
                chatViewModel.sendVideoAttachment(videoUrl)
            },
            onCancel = {
                isRecordingVideo = false
                videoViewModel.cancelRecording()
            }
        )
        return
    }

    // ── Chat overlay: render ChatScreen instead of assignment list ──────
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
        // Teacher profile header card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Face, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = Localization.get("teacher_dashboard_title", lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${Localization.get("role_teacher", lang)}: ${teacher?.username ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = Localization.get("your_code_label", lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = teacherCode,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 18.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = Localization.get("active_label", lang),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Localization.get("copy_code_tip", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Send Custom Assignment Panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
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

                    // Target Student selector
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

                    // Predefined lesson plan templates
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

                    // Assignment Inputs
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

                    // Video Attachment Checkbox
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
                                valueRange = 10f..240f, // slider goes up to 4 mins to test validation constraints!
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )

                            // Constraint Validation: Video maximum of 3 minutes (180 seconds)
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

        // Section header for SENT ASSIGNMENTS History
        item {
            Text(
                text = Localization.get("sent_assignments_log", lang),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        if (sentAssignments.isEmpty()) {
            item {
                Text(
                    text = Localization.get("no_previous_history", lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        items(sentAssignments) { task ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (task.studentUsername == "ALL") Localization.get("all_students", lang).uppercase() else Localization.get("student_prefix", lang) + task.studentUsername,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Chat button
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

                            // Record Video button (teacher)
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

                            IconButton(
                                onClick = { assignmentVM.deleteAssignment(task.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = Localization.get("delete_task", lang),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    if (task.videoTitle.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = Localization.get("attached_video_label_short", lang) + ": ${task.videoTitle} (${task.videoDurationSeconds}s)",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}


// ----------------------------------------------------
// STUDENT WORKSPACE PANEL
// ----------------------------------------------------
@Composable
fun StudentAssignmentsTab(
    assignmentVM: AssignmentViewModel,
    authVM: AuthViewModel,
    sessionManager: SessionManager,
    chatViewModel: ChatViewModel,
    videoViewModel: VideoUploadViewModel? = null,
    onPlayTutorialVideo: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang by sessionManager.appLanguage.collectAsState()
    val student by sessionManager.currentUser.collectAsState()
    val assignments by assignmentVM.studentAssignments.collectAsState()

    var inputTeacherCode by remember { mutableStateOf("") }
    val linkedTeacher = student?.teacherCode ?: ""

    // Chat overlay state: Pair(assignmentId, assignmentTitle)
    var selectedChatAssignment by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Video recording overlay state
    var isRecordingVideo by remember { mutableStateOf(false) }

    // ── Video recording overlay ──────────────────────────────────────
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

    // ── Chat overlay: render ChatScreen instead of assignment list ──────
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
            // STEP 1: FORCE VERIFY TEACHER CODE
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
            // STEP 2: LOAD VERIFIED ASSIGNMENTS ONLY
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header linked info
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
                                    // Chat button
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

                                    // Record Video button (student)
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

// No translator block

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
