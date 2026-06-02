package com.violinmaster.app.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TuningWheel(
    needleAngleOffset: Float,
    pitchOffsetCents: Float,
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val centerX = width / 2f
        val centerY = height * 0.85f
        val needleLength = height * 0.7f
        val arcRadius = height * 0.65f

        // Draw Background Arc
        drawArc(
            color = Color(0xFF49454F),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(centerX - arcRadius, centerY - arcRadius),
            size = size.copy(width = arcRadius * 2, height = arcRadius * 2),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw tick marks inside the arc
        // -50 cents to 50 cents (11 lines from 180deg to 360deg, steps of 18deg)
        for (i in 0..10) {
            val stepAngle = 180f + i * 18f
            val angleRad = Math.toRadians(stepAngle.toDouble())

            val startRadius = arcRadius - 8.dp.toPx()
            val endRadius = arcRadius + 8.dp.toPx()

            val startX = centerX + startRadius * cos(angleRad).toFloat()
            val startY = centerY + startRadius * sin(angleRad).toFloat()
            val endX = centerX + endRadius * cos(angleRad).toFloat()
            val endY = centerY + endRadius * sin(angleRad).toFloat()

            val color = when (i) {
                0, 10 -> Color(0xFFE53935) // Flat/Sharp warnings
                5 -> Color(0xFF81C784) // Perfectly in Tune
                else -> Color(0xFF938F99)
            }
            val thickness = if (i % 5 == 0) 4.dp.toPx() else 1.5.dp.toPx()

            drawLine(
                color = color,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = thickness,
                cap = StrokeCap.Round
            )
        }

        // Draw sweeping analog needle
        // needleAngleOffset goes from -50 (extreme flat) to +50 (extreme sharp).
        // Let's map it to an angle between 210 degrees and 330 degrees (center is 270)
        val angleMap = 270f + (needleAngleOffset / 50f) * 60f
        val needleRad = Math.toRadians(angleMap.toDouble())

        val endNeedleX = centerX + needleLength * cos(needleRad).toFloat()
        val endNeedleY = centerY + needleLength * sin(needleRad).toFloat()

        val isPerfect = Math.abs(pitchOffsetCents) < 2.5f && isListening

        drawLine(
            color = if (isPerfect) Color(0xFF81C784) else Color(0xFFD0BCFF),
            start = Offset(centerX, centerY),
            end = Offset(endNeedleX, endNeedleY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Center Pivot point
        drawCircle(
            color = if (isPerfect) Color(0xFF81C784) else Color(0xFFEADDFF),
            radius = 8.dp.toPx(),
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = Color(0xFF1C1B1F),
            radius = 3.dp.toPx(),
            center = Offset(centerX, centerY)
        )
    }
}
