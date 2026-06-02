package com.violinmaster.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.violinmaster.app.data.PracticeSession
import com.violinmaster.app.ui.viewmodel.PracticeViewModel
import com.violinmaster.app.ui.viewmodel.AssignmentViewModel
import com.violinmaster.app.di.AuthManager
import com.violinmaster.app.di.UserPreferencesManager
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

    var activeSubTab by remember { mutableStateOf("analytics") } // "analytics" or "leaderboard"

    // Process past 7 days of practice minutes
    val calendar = Calendar.getInstance()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

    val chartData = mutableListOf<Pair<String, Float>>() // Pair (Abbreviated Day, Practice Minutes)

    for (i in -6..0) {
        calendar.time = Date()
        calendar.add(Calendar.DAY_OF_YEAR, i)
        val targetDateStr = sdf.format(calendar.time)
        val dayLabel = dayFormat.format(calendar.time)

        // Find and sum daily seconds
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
            // If checking "today" and missed, carry on checking yesterday before breaking streak
            val nowStr = sdf.format(Date())
            if (checkDateStr == nowStr) {
                checkCalendar.add(Calendar.DAY_OF_YEAR, -1)
                val yesterdayStr = sdf.format(checkCalendar.time)
                if (sessions.any { it.dateString == yesterdayStr }) {
                    // Start streak count from yesterday
                    streakCounter = 0 // resetting in case it counted today (impossible)
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
                // --- Aggregated Stats Summary Row ---
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // S1: Streak Card
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStrokeHelper()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "🔥",
                                    fontSize = 24.sp,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = String.format(Localization.get("streak_days_format", appLanguage), streakCounter),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = Localization.get("practice_streak", appLanguage),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        // S2: Average Card
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStrokeHelper()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "📈",
                                    fontSize = 24.sp,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = String.format(Localization.get("average_format", appLanguage), averageMins),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = Localization.get("daily_average", appLanguage),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }

                // --- Custom Weekly Curve Chart ---
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .testTag("weekly_practice_chart_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStrokeHelper()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = Localization.get("practice_drill_trend", appLanguage),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                val primaryColor = MaterialTheme.colorScheme.primary
                                val secondaryColor = MaterialTheme.colorScheme.primaryContainer

                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val width = size.width
                                    val height = size.height

                                    val leftPadding = 24.dp.toPx()
                                    val rightPadding = 12.dp.toPx()
                                    val topPadding = 12.dp.toPx()
                                    val bottomPadding = 20.dp.toPx()

                                    val graphWidth = width - leftPadding - rightPadding
                                    val graphHeight = height - topPadding - bottomPadding

                                    val maxMinutes = (chartData.map { it.second }.maxOrNull() ?: 15f).coerceAtLeast(30f)

                                    // Draw Y-axis guideline grids
                                    repeat(3) { step ->
                                        val gridY = topPadding + graphHeight * (step / 2f)
                                        drawLine(
                                            color = Color(0xFF49454F).copy(alpha = 0.3f),
                                            start = Offset(leftPadding, gridY),
                                            end = Offset(width - rightPadding, gridY),
                                            strokeWidth = 1.dp.toPx()
                                        )
                                    }

                                    // Calculate X and Y coordinate mapping
                                    val points = chartData.mapIndexed { index, data ->
                                        val x = leftPadding + (index.toFloat() / 6f) * graphWidth
                                        val ratio = (data.second / maxMinutes).coerceIn(0f, 1f)
                                        val y = topPadding + (1f - ratio) * graphHeight
                                        Offset(x, y)
                                    }

                                    // Draw Gradient under-line fill path
                                    if (points.isNotEmpty()) {
                                        val fillPath = Path().apply {
                                            moveTo(points.first().x, topPadding + graphHeight)
                                            points.forEach { point ->
                                                lineTo(point.x, point.y)
                                            }
                                            lineTo(points.last().x, topPadding + graphHeight)
                                            close()
                                        }

                                        drawPath(
                                            path = fillPath,
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    primaryColor.copy(alpha = 0.25f),
                                                    Color.Transparent
                                                )
                                            )
                                        )

                                        // Draw sleek continuous Bezier curve
                                        val strokePath = Path().apply {
                                            var pPrev = points.first()
                                            moveTo(pPrev.x, pPrev.y)
                                            for (i in 1 until points.size) {
                                                val pCur = points[i]
                                                val cX = (pPrev.x + pCur.x) / 2f
                                                quadraticTo(pPrev.x, pPrev.y, cX, (pPrev.y + pCur.y) / 2f)
                                                pPrev = pCur
                                            }
                                            lineTo(pPrev.x, pPrev.y)
                                        }

                                        drawPath(
                                            path = strokePath,
                                            color = primaryColor,
                                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                        )

                                        // Draw concentric indicator points with a white center
                                        points.forEachIndexed { i, pt ->
                                            drawCircle(
                                                color = primaryColor,
                                                radius = 5.dp.toPx(),
                                                center = pt
                                            )
                                            drawCircle(
                                                color = Color.White,
                                                radius = 2.dp.toPx(),
                                                center = pt
                                            )
                                        }
                                    }
                                }

                                // Dynamic labels below chart overlay
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomEnd)
                                        .padding(start = 24.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    chartData.forEach { data ->
                                        Text(
                                            text = data.first,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.width(36.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // --- History Logs Label ---
                item {
                    Text(
                        text = Localization.get("completed_practice_journal", appLanguage),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                        letterSpacing = 1.5.sp
                    )
                }

                if (sessions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = Localization.get("empty_log_book", appLanguage),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp)
                            )
                        }
                    }
                }

                items(sessions) { log ->
                    HistoryLogItem(
                        log = log,
                        appLanguage = appLanguage,
                        onDelete = { practiceVM.deleteSession(log.id) }
                    )
                }
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

@Composable
fun HistoryLogItem(log: PracticeSession, appLanguage: AppLanguage, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_log_item_${log.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStrokeHelper()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🎻", fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = log.category,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${log.dateString} • ${String.format(Localization.get("practice_log_format", appLanguage), log.durationSeconds / 60)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_log_${log.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = Localization.get("delete_log_cd", appLanguage),
                    tint = Color(0xFFF2B8B5).copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
