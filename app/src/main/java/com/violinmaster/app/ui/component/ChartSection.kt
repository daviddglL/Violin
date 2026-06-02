package com.violinmaster.app.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.violinmaster.app.ui.screens.BorderStrokeHelper
import com.violinmaster.app.ui.theme.AppLanguage
import com.violinmaster.app.ui.theme.Localization

@Composable
fun ChartSection(
    chartData: List<Pair<String, Float>>,
    appLanguage: AppLanguage
) {
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
