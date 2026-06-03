package com.violinmaster.app.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Gauge wheel that renders the tuning needle arc, tick marks,
 * and overflow labels when the pitch offset exceeds [maxCents].
 *
 * The arc spans 120° total (±60° from vertical). Tick spacing
 * adapts to [maxCents]: one tick every 25 cents, centered at 0.
 * The needle maps linearly from the clamped offset to the arc angle.
 * When |pitchOffsetCents| > maxCents, the needle pins at the
 * extreme edge and overflow labels (e.g., ">-50" / "<+50") appear.
 *
 * @param needleAngleOffset  Smoothed (animated) pitch offset in cents.
 * @param pitchOffsetCents   Raw (unanimated) pitch offset for the
 *                           pinning threshold and "in tune" color logic.
 * @param isListening        Whether the microphone is actively listening.
 * @param maxCents           Maximum cents range for the gauge arc.
 *                           Clamped to [25..200] by the caller.
 * @param modifier           Optional [Modifier] for the root composable.
 */
@Composable
fun TuningWheel(
    needleAngleOffset: Float,
    pitchOffsetCents: Float,
    isListening: Boolean,
    maxCents: Int = 50,
    modifier: Modifier = Modifier
) {
    val effectiveMax = maxCents.coerceIn(25, 200)

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val centerX = width / 2f
        val centerY = height * 0.85f
        val needleLength = height * 0.7f
        val arcRadius = height * 0.65f

        // ── Background arc (180° half-circle) ──
        drawArc(
            color = Color(0xFF49454F),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(centerX - arcRadius, centerY - arcRadius),
            size = size.copy(width = arcRadius * 2, height = arcRadius * 2),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )

        // ── Dynamic tick marks (one every 25 cents) ──
        val ticksPerSide = effectiveMax / 25  // e.g. 2 for 50c, 4 for 100c
        val totalSteps = ticksPerSide * 2     // non-center ticks total

        for (i in 0..totalSteps) {
            val stepCents = -effectiveMax + i * 25  // from -max to +max
            val stepAngle = 270f + (stepCents.toFloat() / effectiveMax.toFloat()) * 60f
            val angleRad = Math.toRadians(stepAngle.toDouble())

            val startRadius = arcRadius - 8.dp.toPx()
            val endRadius = arcRadius + 8.dp.toPx()

            val startX = centerX + startRadius * cos(angleRad).toFloat()
            val startY = centerY + startRadius * sin(angleRad).toFloat()
            val endX = centerX + endRadius * cos(angleRad).toFloat()
            val endY = centerY + endRadius * sin(angleRad).toFloat()

            val color = when {
                i == 0 || i == totalSteps -> Color(0xFFE53935) // Flat/Sharp extreme
                i == ticksPerSide -> Color(0xFF81C784)         // Center = in tune
                else -> Color(0xFF938F99)
            }
            val thickness = if (i == ticksPerSide || i == 0 || i == totalSteps) 4.dp.toPx() else 1.5.dp.toPx()

            drawLine(
                color = color,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = thickness,
                cap = StrokeCap.Round
            )
        }

        // ── Needle ──
        val isPinned = Math.abs(pitchOffsetCents) > effectiveMax.toFloat()
        val clampedOffset = needleAngleOffset.coerceIn(
            -effectiveMax.toFloat(),
            effectiveMax.toFloat()
        )
        val angleMap = 270f + (clampedOffset / effectiveMax.toFloat()) * 60f
        val needleRad = Math.toRadians(angleMap.toDouble())

        val endNeedleX = centerX + needleLength * cos(needleRad).toFloat()
        val endNeedleY = centerY + needleLength * sin(needleRad).toFloat()

        val isPerfect = Math.abs(pitchOffsetCents) < 2.5f && isListening

        val needleColor = when {
            isPerfect -> Color(0xFF81C784)
            isPinned -> Color(0xFFFFB74D)  // Amber — overflow
            else -> Color(0xFFD0BCFF)
        }

        drawLine(
            color = needleColor,
            start = Offset(centerX, centerY),
            end = Offset(endNeedleX, endNeedleY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // ── Center pivot ──
        val pivotColor = when {
            isPerfect -> Color(0xFF81C784)
            isPinned -> Color(0xFFFFB74D)
            else -> Color(0xFFEADDFF)
        }
        drawCircle(
            color = pivotColor,
            radius = 8.dp.toPx(),
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = Color(0xFF1C1B1F),
            radius = 3.dp.toPx(),
            center = Offset(centerX, centerY)
        )

        // ── Overflow labels at arc edges ──
        val overflowPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(229, 57, 53) // Color(0xFFE53935)
            textSize = 10.dp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }

        val labelRadius = arcRadius + 20.dp.toPx()
        val extremeAngleLeft = Math.toRadians(210.0)   // -60° from vertical
        val extremeAngleRight = Math.toRadians(330.0)  // +60° from vertical

        val leftLabelX = centerX + labelRadius * cos(extremeAngleLeft).toFloat()
        val leftLabelY = centerY + labelRadius * sin(extremeAngleLeft).toFloat()
        val rightLabelX = centerX + labelRadius * cos(extremeAngleRight).toFloat()
        val rightLabelY = centerY + labelRadius * sin(extremeAngleRight).toFloat()

        drawContext.canvas.nativeCanvas.drawText(
            "\u2212$effectiveMax",   // −50, −100, etc.
            leftLabelX, leftLabelY,
            overflowPaint
        )
        drawContext.canvas.nativeCanvas.drawText(
            "+$effectiveMax",
            rightLabelX, rightLabelY,
            overflowPaint
        )
    }
}
