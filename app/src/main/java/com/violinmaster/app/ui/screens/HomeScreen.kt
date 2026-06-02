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
import com.violinmaster.app.ui.component.DailyTasksSection
import com.violinmaster.app.ui.component.PracticeTimerControls
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_points_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userVal.username.take(1).uppercase(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = userVal.username,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = Localization.get("level_title", appLanguage) + ": ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val skillLevelText = when (userVal.skillLevel) {
                                    "Beginner" -> Localization.get("skill_beginner", appLanguage)
                                    "Intermediate" -> Localization.get("skill_intermediate", appLanguage)
                                    "Advanced" -> Localization.get("skill_advanced", appLanguage)
                                    else -> userVal.skillLevel
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.clickable {
                                        val nextLevel = when (userVal.skillLevel) {
                                            "Beginner" -> "Intermediate"
                                            "Intermediate" -> "Advanced"
                                            else -> "Beginner"
                                        }
                                        practiceVM.updateSkillLevel(nextLevel)
                                    }
                                        .testTag("cycle_level_button")
                                ) {
                                    Text(
                                        text = "$skillLevelText 🔄",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    Surface(
                        color = Color(0xFFFFD700).copy(alpha = 0.15f),
                        contentColor = Color(0xFFFFD700),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700)),
                        modifier = Modifier.testTag("user_points_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🏆", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${userVal.points} " + Localization.get("points_suffix", appLanguage),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }

        // --- Practice Progress Card (Hero) ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("practice_progress_card"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = Localization.get("daily_goal_title", appLanguage),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$todayFinishedMinutes",
                                style = MaterialTheme.typography.headlineLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Light,
                                fontSize = 42.sp
                            )
                            Text(
                                text = " / $dailyGoalMinutes min",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                            )
                        }
                    }

                    // Progress circular count
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.size(64.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            strokeWidth = 6.dp,
                        )
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.size(64.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 6.dp,
                        )
                        Text(
                            text = "${(progressPercent * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Multi-segment progress bar visualization
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) { index ->
                        val segmentThreshold = (index + 1) * 0.33f
                        val alpha = if (progressPercent >= segmentThreshold) 1.0f else if (progressPercent >= segmentThreshold - 0.33f) 0.4f else 0.15f
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
                        )
                    }
                }
            }
        }

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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                        )
                    )
                )
                .testTag("active_lesson_card")
        ) {
            // Mini Label badge
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Text(
                    text = Localization.get("current_badge", appLanguage),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = Localization.get("module_3_label", appLanguage),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Localization.get("advanced_bowing_title", appLanguage),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 28.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = Localization.get("advanced_bowing_desc", appLanguage),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        lineHeight = 18.sp
                    )
                }

                Button(
                    onClick = {
                        practiceVM.startPracticeTimer("Advanced Bowing: Détaché & Martelé")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("resume_practice_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isPracticing && practiceCategory == "Advanced Bowing: Détaché & Martelé")
                                Localization.get("practicing_now_label", appLanguage)
                            else
                                Localization.get("resume_practice_button", appLanguage),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("→", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

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
