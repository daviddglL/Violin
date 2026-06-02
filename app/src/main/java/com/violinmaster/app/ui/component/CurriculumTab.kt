package com.violinmaster.app.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.violinmaster.app.data.LessonProgress
import com.violinmaster.app.di.NavigationManager
import com.violinmaster.app.ui.screens.BorderStrokeHelper
import com.violinmaster.app.ui.theme.AppLanguage
import com.violinmaster.app.ui.theme.Localization
import com.violinmaster.app.ui.viewmodel.PracticeViewModel

// ----------------------------------------------------
// EXTRA SUBVIEWS & COMPONENTS
// ----------------------------------------------------

@Composable
fun CurriculumTab(
    levelProgressList: List<LessonProgress>,
    isPracticing: Boolean,
    practiceCategory: String,
    appLanguage: AppLanguage = AppLanguage.ENGLISH,
    practiceVM: PracticeViewModel,
    navigationManager: NavigationManager
) {
    val groupedLessons = levelProgressList.groupBy { it.difficulty }
    val difficultyOrder = listOf("Beginner", "Intermediate", "Advanced")

    // Dynamic expanded item tracking
    var expandedLessonId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ----------------------------------------------------
        // JOURNEY OVERVIEW DASHBOARD PANEL
        // ----------------------------------------------------
        item {
            val totalSeconds = levelProgressList.sumOf { it.totalPracticedSeconds }
            val completedCount = levelProgressList.count { it.completed }
            val totalCount = levelProgressList.size.coerceAtLeast(1)
            val completionPercentageValue = (completedCount.toFloat() / totalCount.toFloat())
            val progressAnim by animateFloatAsState(targetValue = completionPercentageValue, label = "Progress Circle")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("journey_tracker_dashboard"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStrokeHelper()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Circular Progress Ring
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(80.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { progressAnim },
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 8.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        Text(
                            text = "${(completedCount * 100) / totalCount}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Right Summary Metadata details
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = Localization.get("learning_journey_progress", appLanguage),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Localization.get("modules_progress_format", appLanguage), completedCount, totalCount),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = String.format(Localization.get("total_bow_time_format", appLanguage), totalSeconds / 60),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        // Dynamic encouraging message
                        Text(
                            text = when {
                                completionPercentageValue <= 0.25f -> Localization.get("encouragement_seed", appLanguage)
                                completionPercentageValue <= 0.60f -> Localization.get("encouragement_warm", appLanguage)
                                completionPercentageValue <= 0.85f -> Localization.get("encouragement_habits", appLanguage)
                                else -> Localization.get("encouragement_virtuoso", appLanguage)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        // Seeding groups
        for (difficulty in difficultyOrder) {
            val lessons = groupedLessons[difficulty] ?: emptyList()
            if (lessons.isNotEmpty()) {
                item {
                    Text(
                        text = String.format(Localization.get("syllabus_suffix", appLanguage), difficulty.uppercase()),
                        style = MaterialTheme.typography.labelLarge,
                        color = when (difficulty) {
                            "Beginner" -> Color(0xFF81C784)
                            "Intermediate" -> MaterialTheme.colorScheme.primary
                            else -> Color(0xFFF2B8B5)
                        },
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                items(lessons) { lesson ->
                    val extraDetails = lessonDetailsMap[lesson.lessonId]
                    val isExpanded = expandedLessonId == lesson.lessonId

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lesson_item_${lesson.lessonId}")
                            .clickable {
                                expandedLessonId = if (isExpanded) null else lesson.lessonId
                            },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = if (isExpanded) {
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            BorderStrokeHelper()
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Row Header (Title, check indicator)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (lesson.completed) "✅" else "🎻",
                                    fontSize = 20.sp,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = lesson.lessonTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = String.format(Localization.get("level_format", appLanguage), difficulty) + " • ",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = String.format(Localization.get("minutes_practiced_format", appLanguage), lesson.totalPracticedSeconds / 60),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = if (isExpanded) "▲" else "▼",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            // Expandable Details Section
                            AnimatedVisibility(visible = isExpanded) {
                                Column(modifier = Modifier.padding(top = 16.dp)) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (extraDetails != null) {
                                        // Description
                                        Text(
                                            text = extraDetails.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = 18.sp
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Explicit Learning Objectives
                                    Text(
                                        text = "🎯 " + Localization.get("stage_learning_objectives", appLanguage),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        extraDetails.objectives.forEach { obj ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 2.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Text(
                                                    text = "✓",
                                                    color = Color(0xFF81C784),
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(end = 6.dp)
                                                )
                                                Text(
                                                    text = obj,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Sub-topics checklist
                                    Text(
                                        text = "📝 " + Localization.get("included_core_drills", appLanguage),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary,
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        extraDetails.subtopics.forEach { sub ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "◽",
                                                    modifier = Modifier.padding(end = 6.dp),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = sub,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Activity Action Buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Manual Status Toggle Complete
                                        Button(
                                            onClick = { practiceVM.toggleLessonStatus(lesson.lessonId, !lesson.completed) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (lesson.completed) Color(0xFF2E7D32).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = if (lesson.completed) Color(0xFF81C784) else MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.testTag("check_lesson_${lesson.lessonId}")
                                        ) {
                                            Text(if (lesson.completed) "✓ " + Localization.get("lesson_completed_button", appLanguage) else Localization.get("mark_done_button", appLanguage))
                                        }

                                        // Start Live Practice Session Timer
                                        val isCurrent = isPracticing && practiceCategory == lesson.lessonTitle
                                        Button(
                                            onClick = {
                                                practiceVM.startPracticeTimer(lesson.lessonTitle)
                                                navigationManager.selectTab(0) // redirect home to see timer sweeping
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isCurrent) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier.testTag("practice_button_${lesson.lessonId}")
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isCurrent) "▶ " + Localization.get("practicing_label", appLanguage) else Localization.get("drill_time_button", appLanguage))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
