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
import com.violinmaster.app.ui.viewmodel.PracticeViewModel
import com.violinmaster.app.di.SessionManager
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.violinmaster.app.ui.theme.Localization

data class DailyTaskItem(
    val id: String,
    val titleEn: String,
    val titleEs: String,
    val descEn: String,
    val descEs: String,
    val category: String
)

val beginnerDailyTasks = listOf(
    DailyTaskItem("beg_dt1", "Open Resonance Bowing", "Alineación y Arcos de Cuerda al Aire", "Practice full whole bows on open strings D and A.", "Practica arcos enteros en cuerdas libres Re y La.", "Open Strings Tuning & Bowing"),
    DailyTaskItem("beg_dt2", "Bow Hold Pinky Taps", "Toques de Meñique en el Arco", "Perform 15 clean pinky taps to build finger flexibility.", "Realiza 15 toques de meñique para ganar flexibilidad.", "Posture Check & Bow Grip"),
    DailyTaskItem("beg_dt3", "Precision Ear Tuner", "Afinación Auditiva de Precisión", "Match 3 strings to the smart tuner perfect pitches.", "Sincroniza 3 cuerdas al aire con el afinador inteligente.", "Smart Tuner Tuning")
)

val intermediateDailyTasks = listOf(
    DailyTaskItem("int_dt1", "Smooth Shifting Slide", "Deslizamiento Fa# en 3ª Posición", "Glide finger 2 up to third position and verify pitch.", "Desliza el dedo 2 a la tercera posición y analiza el tono.", "Shifting to Third Position (III)"),
    DailyTaskItem("int_dt2", "Warm Pulsed Vibrato", "Vibrato de Calor Pulsado", "Oscillate fingers on the G string for 1 minute.", "Oscila los dedos en cuerda Sol con vibrato por 1 minuto.", "Relaxing Left Hand & Vibrato"),
    DailyTaskItem("int_dt3", "Double Stop Stability", "Estabilidad en Doble Cuerda", "Play fourths & fifths balancing bow hair weight.", "Toca cuartas y quintas equilibrando el peso del arco.", "Double Stop Balance & Harmony")
)

val advancedDailyTasks = listOf(
    DailyTaskItem("adv_dt1", "Gravity Spiccato Nodes", "Rebotes de Madera Spiccato", "Bounce the bow rapid sixteenths at 110 BPM.", "Rebota el arco en velocidad de semicorcheas a 110 BPM.", "Bowing Styles: Martelé, Spiccato"),
    DailyTaskItem("adv_dt2", "Violin Neck Extreme Shift", "Cambio Extremo de Diapasón", "Perform shifts to 5th position on the A string.", "Realiza cambios a la quinta posición en la cuerda La.", "High Position Shifts (5th & 7th)"),
    DailyTaskItem("adv_dt3", "Paganini Velocity Run", "Arpegio del Diablo Paganini", "Coordinate rapid string crossings cleanly.", "Coordina arpegios rápidos en cruzado de cuerdas limpio.", "Paganini Practice Theme (A minor)")
)

@Composable
fun HomeScreen(
    practiceVM: PracticeViewModel,
    sessionManager: SessionManager,
    modifier: Modifier = Modifier
) {
    val todayFinishedSeconds by practiceVM.todayFinishedSeconds.collectAsState()
    val dailyGoalMinutes by practiceVM.dailyGoalMinutes.collectAsState()

    val isPracticing by practiceVM.isPracticing.collectAsState()
    val practiceCategory by practiceVM.practiceCategoryName.collectAsState()
    val practiceElapsed by practiceVM.practiceElapsedSeconds.collectAsState()

    val userAccount by sessionManager.currentUser.collectAsState()
    val appLanguage by sessionManager.appLanguage.collectAsState()
    val dailyTasksCompleted by practiceVM.dailyTasksCompleted.collectAsState()

    var activeTaskForCompletion by remember { mutableStateOf<DailyTaskItem?>(null) }
    var selectedAttemptsCount by remember { mutableStateOf(1) }

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
        AnimatedVisibility(visible = isPracticing) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("active_timer_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = Localization.get("active_practice_label", appLanguage),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = practiceCategory,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val mins = practiceElapsed / 60
                    val secs = practiceElapsed % 60
                    val timerDisplay = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

                    Text(
                        text = timerDisplay,
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium,
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { practiceVM.stopAndSavePracticeSession() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                contentColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("save_practice_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = Localization.get("save_practice_cd", appLanguage))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Localization.get("save_and_log_button", appLanguage), fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = { practiceVM.cancelPracticeTimer() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFC53030),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("cancel_practice_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = Localization.get("cancel_practice_cd", appLanguage))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Localization.get("discard_button", appLanguage))
                        }
                    }
                }
            }
        }

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
                    .clickable { sessionManager.showOverlay("tuner") }
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
                    .clickable { sessionManager.showOverlay("metronome") }
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
        val userLevel = userAccount?.skillLevel ?: "Beginner"
        val dailyTasksList = when (userLevel) {
            "Intermediate" -> intermediateDailyTasks
            "Advanced" -> advancedDailyTasks
            else -> beginnerDailyTasks
        }

        Text(
            text = Localization.get("daily_tasks_title", appLanguage),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            letterSpacing = 1.5.sp
        )

        dailyTasksList.forEach { task ->
            val isCompleted = dailyTasksCompleted.contains(task.id)
            val titleText = if (appLanguage == com.violinmaster.app.ui.theme.AppLanguage.SPANISH) task.titleEs else task.titleEn
            val descText = if (appLanguage == com.violinmaster.app.ui.theme.AppLanguage.SPANISH) task.descEs else task.descEn

            Card(
                modifier = Modifier.fillMaxWidth()
                    .testTag("daily_task_item_${task.id}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isCompleted) Color(0xFF81C784).copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isCompleted) {
                                Text("✅", fontSize = 16.sp, modifier = Modifier.padding(end = 6.dp))
                            } else {
                                Text("🎯", fontSize = 16.sp, modifier = Modifier.padding(end = 6.dp))
                            }
                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isCompleted) Color(0xFF81C784) else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = descText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    if (isCompleted) {
                        Surface(
                            color = Color(0xFF81C784).copy(alpha = 0.15f),
                            contentColor = Color(0xFF81C784),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = Localization.get("completed_btn", appLanguage),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.End) {
                            Button(
                                onClick = {
                                    practiceVM.startPracticeTimer(task.category)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = Localization.get("start_task", appLanguage),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    activeTaskForCompletion = task
                                    selectedAttemptsCount = 1
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                                    .testTag("complete_task_${task.id}")
                            ) {
                                Text(
                                    text = "✓",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // --- Attempts selection Dialog ---
    val taskToComplete = activeTaskForCompletion
    if (taskToComplete != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { activeTaskForCompletion = null },
            title = {
                Text(
                    text = Localization.get("attempts_needed_title", appLanguage),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = Localization.get("attempts_needed_subtitle", appLanguage),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    listOf(1, 2, 3, 4).forEach { attemptOption ->
                        val label = when (attemptOption) {
                            1 -> Localization.get("attempt_1", appLanguage)
                            2 -> Localization.get("attempt_2", appLanguage)
                            3 -> Localization.get("attempt_3", appLanguage)
                            else -> Localization.get("attempt_4", appLanguage)
                        }
                        val isSelected = selectedAttemptsCount == attemptOption
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAttemptsCount = attemptOption }
                                .clip(RoundedCornerShape(8.dp)),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedAttemptsCount = attemptOption }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        practiceVM.completeDailyTask(taskToComplete.id, selectedAttemptsCount)
                        activeTaskForCompletion = null
                    }
                ) {
                    Text(
                        text = Localization.get("confirm_completion", appLanguage),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { activeTaskForCompletion = null }
                ) {
                    Text(text = Localization.get("cancel_button", appLanguage))
                }
            }
        )
    }
}
