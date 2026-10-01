package com.budgetmanager.app.core.designsystem.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import com.budgetmanager.app.domain.DonutSlice
import kotlin.math.cos
import kotlin.math.sin

/**
 * The Trends "This month" ring: one arc per [DonutSlice], sized by its share of the total budget,
 * with the used portion coloured by status and the rest left as the track colour (02-features.md
 * F10). Drawn on Canvas rather than a charting library, per 14-implementation-plan.md M10 - this
 * app has exactly two chart types total, not worth a dependency for.
 */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    centerPercentText: String,
    centerSubText: String,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val emojiPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
    }

    Box(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // The ring and its labels must both fit inside size.minDimension / 2 - Canvas does not
            // clip its own drawing, so a label radius past that bleeds into whatever is laid out
            // below this composable (the legend row). Leaves ~15% of the radius as label margin.
            val strokeWidth = size.minDimension * 0.12f
            val diameter = size.minDimension * 0.58f
            val labelRadius = size.minDimension * 0.42f
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            emojiPaint.textSize = strokeWidth * 1f

            var startAngle = -90f
            slices.forEach { slice ->
                val sweep = slice.budgetShare * 360f
                if (sweep > 0f) {
                    drawArc(
                        color = trackColor,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth)
                    )
                    val usedSweep = sweep * slice.usedFraction
                    if (usedSweep > 0f) {
                        drawArc(
                            color = slice.status.color(),
                            startAngle = startAngle,
                            sweepAngle = usedSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth)
                        )
                    }

                    val midAngleRad = Math.toRadians((startAngle + sweep / 2f).toDouble())
                    val labelX = size.width / 2f + (labelRadius * cos(midAngleRad)).toFloat()
                    val labelY = size.height / 2f + (labelRadius * sin(midAngleRad)).toFloat() +
                        emojiPaint.textSize * 0.35f // nudge down to vertically centre the glyph on its baseline
                    emojiPaint.color = trackColor.toArgb()
                    drawContext.canvas.nativeCanvas.drawText(slice.emoji, labelX, labelY, emojiPaint)

                    startAngle += sweep
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                centerPercentText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                centerSubText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
