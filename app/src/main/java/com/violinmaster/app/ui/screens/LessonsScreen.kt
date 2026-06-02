package com.violinmaster.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.violinmaster.app.di.AuthManager
import com.violinmaster.app.di.NavigationManager
import com.violinmaster.app.di.UserPreferencesManager
import com.violinmaster.app.ui.component.CurriculumTab
import com.violinmaster.app.ui.component.LessonVideoPlayer
import com.violinmaster.app.ui.component.StudentAssignmentsTab
import com.violinmaster.app.ui.component.TeacherDashboardTab
import com.violinmaster.app.ui.component.TheoryQuizTab
import com.violinmaster.app.ui.component.VirtualFingerboard
import com.violinmaster.app.ui.theme.AppLanguage
import com.violinmaster.app.ui.theme.Localization
import com.violinmaster.app.ui.viewmodel.AssignmentViewModel
import com.violinmaster.app.ui.viewmodel.AuthViewModel
import com.violinmaster.app.ui.viewmodel.ChatViewModel
import com.violinmaster.app.ui.viewmodel.PracticeViewModel
import com.violinmaster.app.ui.viewmodel.TunerViewModel

// ----------------------------------------------------
// MAIN SCREEN IMPLEMENTATION
// ----------------------------------------------------
@Composable
fun LessonsScreen(
    practiceVM: PracticeViewModel,
    tunerVM: TunerViewModel,
    authVM: AuthViewModel,
    assignmentVM: AssignmentViewModel,
    userPreferencesManager: UserPreferencesManager,
    authManager: AuthManager,
    navigationManager: NavigationManager,
    chatViewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val lang by userPreferencesManager.appLanguage.collectAsState()
    val user by authManager.currentUser.collectAsState()
    val levelProgressList by practiceVM.allLevelProgress.collectAsState()
    val isPracticing by practiceVM.isPracticing.collectAsState()
    val practiceCategory by practiceVM.practiceCategoryName.collectAsState()

    var activeTabSubIndex by rememberSaveable { mutableStateOf(0) } // 0: Curriculum, 1: Fingerboard, 2: Theory Quiz, 3: Masterclass

    // Student tutor video target states
    var activeTutorVideoUrl by remember { mutableStateOf<String?>(null) }
    var activeTutorVideoTitle by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (activeTutorVideoUrl != null && activeTutorVideoTitle != null) {
            LessonVideoPlayer(
                videoTitle = activeTutorVideoTitle ?: "",
                signedUrl = activeTutorVideoUrl ?: "",
                onClose = {
                    activeTutorVideoUrl = null
                    activeTutorVideoTitle = null
                },
                appLanguage = lang
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Shared Screen Header
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = Localization.get("tab_lessons", lang).uppercase(java.util.Locale.ROOT),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = when (activeTabSubIndex) {
                            0 -> {
                                when (user?.role) {
                                    "TEACHER" -> Localization.get("teacher_dashboard_title", lang)
                                    "STUDENT" -> Localization.get("student_dashboard_title", lang)
                                    else -> Localization.get("lessons_header_freelancer", lang)
                                }
                            }
                            1 -> Localization.get("smart_tuner", lang)
                            2 -> Localization.get("theory_quest_quiz", lang)
                            else -> Localization.get("premium_masterclass", lang)
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (activeTabSubIndex) {
                            0 -> {
                                when (user?.role) {
                                    "TEACHER" -> Localization.get("teacher_tab_desc", lang)
                                    "STUDENT" -> Localization.get("student_tab_desc", lang)
                                    else -> Localization.get("freelancer_tab_desc", lang)
                                }
                            }
                            1 -> Localization.get("tuner_desc", lang)
                            2 -> Localization.get("theory_quiz_desc", lang)
                            else -> Localization.get("premium_masterclass", lang)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Premium Navigation Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabLabels = listOf(
                        if (user?.role == "TEACHER") "🎓 INSTRUCT" else "📖 LESSONS",
                        "🎯 NECK",
                        "💡 QUIZ",
                        "🔒 VIDEOS"
                    )
                    tabLabels.forEachIndexed { index, title ->
                        val isSelected = activeTabSubIndex == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { activeTabSubIndex = index }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Content Switch
                when (activeTabSubIndex) {
                    0 -> {
                        when (user?.role) {
                            "TEACHER" -> TeacherDashboardTab(assignmentVM = assignmentVM, userPreferencesManager = userPreferencesManager, authManager = authManager, chatViewModel = chatViewModel)
                            "STUDENT" -> StudentAssignmentsTab(
                                assignmentVM = assignmentVM,
                                authVM = authVM,
                                userPreferencesManager = userPreferencesManager,
                                authManager = authManager,
                                chatViewModel = chatViewModel,
                                onPlayTutorialVideo = { url, title ->
                                    activeTutorVideoUrl = url
                                    activeTutorVideoTitle = title
                                }
                            )
                            else -> CurriculumTab(
                                levelProgressList = levelProgressList,
                                isPracticing = isPracticing,
                                practiceCategory = practiceCategory,
                                appLanguage = lang,
                                practiceVM = practiceVM,
                                navigationManager = navigationManager
                            )
                        }
                    }
                    1 -> VirtualFingerboard(tunerVM = tunerVM, appLanguage = lang)
                    2 -> TheoryQuizTab(practiceVM = practiceVM, userPreferencesManager = userPreferencesManager)
                    3 -> MasterclassTab(authViewModel = authVM)
                }
            }
        }
    }
}
