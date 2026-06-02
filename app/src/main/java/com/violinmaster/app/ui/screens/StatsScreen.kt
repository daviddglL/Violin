package com.violinmaster.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.violinmaster.app.ui.component.ChartSection
import com.violinmaster.app.ui.component.StatsSummary
import com.violinmaster.app.ui.component.practiceHistorySection
import com.violinmaster.app.ui.viewmodel.PracticeViewModel
import com.violinmaster.app.ui.viewmodel.AssignmentViewModel
import com.violinmaster.app.di.AuthManager
import com.violinmaster.app.di.UserPreferencesManager
import com.violinmaster.app.ui.theme.Localization
import com.violinmaster.app.ui.theme.AppLanguage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun StatsScreen(
    practiceVM: PracticeViewModel,
    assignmentVM: AssignmentViewModel,
    userPreferencesManager: UserPreferencesManager,
    authManager: AuthManager,
    modifier: Modifier = Modifier
) {
    val sessions by practiceVM.allSessions.collectAsState()
    val appLanguage by userPreferencesManager.appLanguage.collectAsState()
    val allUsers by assignmentVM.allUsers.collectAsState()
    val userAccount by authManager.currentUser.collectAsState()

    var activeSubTab by remember { mutableStateOf("analytics") }
    // Process past 7 days of practice minutes
    val calendar = Calendar.getInstance()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

    val chartData = mutableListOf<Pair<String, Float>>()

    for (i in -6..0) {
        calendar.time = Date()
        calendar.add(Calendar.DAY_OF_YEAR, i)
        val targetDateStr = sdf.format(calendar.time)
        val dayLabel = dayFormat.format(calendar.time)

        val dailySeconds = sessions.filter { it.dateString == targetDateStr }.sumOf { it.durationSeconds }
        val dailyMinutes = dailySeconds / 60f
        chartData.add(Pair(dayLabel, dailyMinutes))
    }

    val totalPracticedMins = sessions.sumOf { it.durationSeconds } / 60
    val averageMins = if (chartData.isNotEmpty()) {
        chartData.map { it.second }.average().toFloat()
    } else {
        0f
    }

    // Crude calculation of streaks looking back day-by-day
    var streakCounter = 0
    val checkCalendar = Calendar.getInstance()
    var checking = true
    while (checking) {
        val checkDateStr = sdf.format(checkCalendar.time)
        val foundPracticed = sessions.any { it.dateString == checkDateStr }
        if (foundPracticed) {
            streakCounter++
            checkCalendar.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            val nowStr = sdf.format(Date())
            if (checkDateStr == nowStr) {
                checkCalendar.add(Calendar.DAY_OF_YEAR, -1)
                val yesterdayStr = sdf.format(checkCalendar.time)
                if (sessions.any { it.dateString == yesterdayStr }) {
                    streakCounter = 0
                } else {
                    checking = false
                }
            } else {
                checking = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                val titleLabel = if (activeSubTab == "analytics") {
                    Localization.get("stats_analytics_title", appLanguage)
                } else {
                    Localization.get("stats_leaderboard_title", appLanguage)
                }
                Text(
                    text = titleLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Tab switcher selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("analytics", "leaderboard").forEach { tabId ->
                        val label = if (tabId == "analytics") {
                            Localization.get("analytics_sub_tab", appLanguage)
                        } else {
                            Localization.get("ranking_sub_tab", appLanguage)
                        }
                        val isSelected = activeSubTab == tabId
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { activeSubTab = tabId }
                                .testTag("stats_tab_$tabId"),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (activeSubTab == "analytics") {
                // Aggregated Stats Summary Row
                item {
                    StatsSummary(
                        streakCounter = streakCounter,
                        averageMins = averageMins,
                        appLanguage = appLanguage
                    )
                }

                // Custom Weekly Curve Chart
                item {
                    ChartSection(
                        chartData = chartData,
                        appLanguage = appLanguage
                    )
                }

                // History Logs section
                practiceHistorySection(sessions, appLanguage, { practiceVM.deleteSession(it) })
            }

            if (activeSubTab == "leaderboard") {
                item {
                    val seededMaestros = listOf(
                        com.violinmaster.app.data.UserAccount("Francesco G.", "FREELANCER", "", "", "", 2500, "Advanced"),
                        com.violinmaster.app.data.UserAccount("Sarah Chang", "STUDENT", "", "", "", 1800, "Advanced"),
                        com.violinmaster.app.data.UserAccount("Yehudi Menuhin", "FREELANCER", "", "", "", 1200, "Advanced"),
                        com.violinmaster.app.data.UserAccount("Arcangelo Corelli", "TEACHER", "", "", "", 950, "Advanced"),
                        com.violinmaster.app.data.UserAccount("Itzhak Perlman", "FREELANCER", "", "", "", 600, "Advanced"),
                        com.violinmaster.app.data.UserAccount("Joshua Bell", "STUDENT", "", "", "", 350, "Intermediate"),
                        com.violinmaster.app.data.UserAccount("David Oistrakh", "FREELANCER", "", "", "", 150, "Beginner")
                    )

                    val combinedList = (allUsers + seededMaestros)
                        .distinctBy { it.username.lowercase() }
                        .sortedByDescending { it.points }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = Localization.get("leaderboard_title", appLanguage),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = Localization.get("rank_label", appLanguage).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = Localization.get("points_label", appLanguage).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val currentUserUsername = userAccount?.username ?: ""
                        combinedList.forEachIndexed { index, userEntry ->
                            val isThisUser = userEntry.username.equals(currentUserUsername, ignoreCase = true)
                            val rankNumber = index + 1
                            val rankBadge = when (rankNumber) {
                                1 -> "🥇"
                                2 -> "🥈"
                                3 -> "🥉"
                                else -> "  #$rankNumber "
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("leaderboard_card_$rankNumber"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isThisUser) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                                ),
                                border = if (isThisUser) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = rankBadge,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(42.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = userEntry.username,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = if (isThisUser) MaterialTheme.colorScheme.primary else Color.White,
                                                    fontWeight = if (isThisUser) FontWeight.Bold else FontWeight.Medium
                                                )
                                                if (isThisUser) {
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.primary,
                                                        shape = CircleShape,
                                                        modifier = Modifier.padding(start = 6.dp)
                                                    ) {
                                                        Text(
                                                            text = if (appLanguage == com.violinmaster.app.ui.theme.AppLanguage.SPANISH) "TÚ" else "YOU",
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = MaterialTheme.colorScheme.onPrimary,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = when (userEntry.skillLevel) {
                                                    "Beginner" -> Localization.get("skill_beginner", appLanguage)
                                                    "Intermediate" -> Localization.get("skill_intermediate", appLanguage)
                                                    "Advanced" -> Localization.get("skill_advanced", appLanguage)
                                                    else -> userEntry.skillLevel
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${userEntry.points} " + Localization.get("points_suffix", appLanguage),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isThisUser) MaterialTheme.colorScheme.primary else Color.White
                                    )
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
}
