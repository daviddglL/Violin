package com.violinmaster.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import com.violinmaster.app.ui.component.ActiveLessonCard
import com.violinmaster.app.ui.component.DailyTasksSection
import com.violinmaster.app.ui.component.PracticeProgressCard
import com.violinmaster.app.ui.component.PracticeTimerControls
import com.violinmaster.app.ui.component.ProfilePointsCard
import com.violinmaster.app.ui.viewmodel.PracticeViewModel
import com.violinmaster.app.di.NavigationManager
import com.violinmaster.app.di.UserPreferencesManager
import com.violinmaster.app.di.AuthManager
import com.violinmaster.app.ui.theme.Localization

@Composable
fun HomeScreen(
    practiceVM: PracticeViewModel,
    authManager: AuthManager,
    userPreferencesManager: UserPreferencesManager,
    navigationManager: NavigationManager,
    modifier: Modifier = Modifier
) {
    val todayFinishedSeconds by practiceVM.todayFinishedSeconds.collectAsState()
    val dailyGoalMinutes by practiceVM.dailyGoalMinutes.collectAsState()

    val isPracticing by practiceVM.isPracticing.collectAsState()
    val practiceCategory by practiceVM.practiceCategoryName.collectAsState()
    val practiceElapsed by practiceVM.practiceElapsedSeconds.collectAsState()

    val userAccount by authManager.currentUser.collectAsState()
    val appLanguage by userPreferencesManager.appLanguage.collectAsState()
    val dailyTasksCompleted by practiceVM.dailyTasksCompleted.collectAsState()

    val todayFinishedMinutes = todayFinishedSeconds / 60
    val progressPercent = if (dailyGoalMinutes > 0) {
        (todayFinishedMinutes.toFloat() / dailyGoalMinutes.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val animatedProgress by animateFloatAsState(targetValue = progressPercent, label = "Goal Progress")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // --- Profile, Points & Level Setup Hero Header ---
        val userVal = userAccount
        if (userVal != null) {
            ProfilePointsCard(
                username = userVal.username,
                points = userVal.points,
                skillLevel = userVal.skillLevel,
                readOnly = true,
                appLanguage = appLanguage
            )
        }

        // --- Take Quiz Button (QA-003) ---
        Button(
            onClick = { navigationManager.navigateToQuizTab() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("take_quiz_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = Localization.get("take_quiz_button", appLanguage),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // --- Practice Progress Card (Hero) ---
        PracticeProgressCard(
            todayFinishedMinutes = todayFinishedMinutes,
            dailyGoalMinutes = dailyGoalMinutes,
            progressPercent = progressPercent,
            animatedProgress = animatedProgress,
            appLanguage = appLanguage
        )

        // --- Ticking Practice Overlay Section (Shows when timer is running) ---
        PracticeTimerControls(
            practiceVM = practiceVM,
            practiceCategory = practiceCategory,
            practiceElapsed = practiceElapsed,
            appLanguage = appLanguage,
            visible = isPracticing
        )

        // --- Quick Tools Grid ---
        Text(
            text = Localization.get("quick_tools_label", appLanguage),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            letterSpacing = 1.5.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Smart Tuner tool button
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(96.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { navigationManager.showOverlay("tuner") }
                    .testTag("tuner_tool_button"),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = ButtonDefaults.outlinedButtonBorder(enabled = true),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎵", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = Localization.get("smart_tuner", appLanguage),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Metronome tool button
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(96.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { navigationManager.showOverlay("metronome") }
                    .testTag("metronome_tool_button"),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = ButtonDefaults.outlinedButtonBorder(enabled = true),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "⏱", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = Localization.get("harmonic_metronome", appLanguage),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // --- Active Lesson Card ---
        Text(
            text = Localization.get("recommended_drill_label", appLanguage),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            letterSpacing = 1.5.sp
        )

        ActiveLessonCard(
            isPracticing = isPracticing,
            practiceCategory = practiceCategory,
            onResumePractice = {
                practiceVM.startPracticeTimer("Advanced Bowing: Détaché & Martelé")
            },
            appLanguage = appLanguage
        )

        // --- Daily Tasks Section based on active Skill Level ---
        DailyTasksSection(
            practiceVM = practiceVM,
            appLanguage = appLanguage,
            skillLevel = userAccount?.skillLevel ?: "Beginner",
            dailyTasksCompleted = dailyTasksCompleted
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
