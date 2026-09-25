package com.budgetmanager.app.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.core.model.BudgetStatus
import com.budgetmanager.app.ui.theme.StatusColors
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** One bar for [PercentBarChart] - Trends' "Previous month" tab. [percent] can exceed 1f for an
 *  over-budget month; the chart draws past the 100% line rather than clamping. */
data class PercentBar(
    val label: String,
    val percent: Float,
    val status: BudgetStatus,
    val isInProgress: Boolean,
    val valueLabel: String?
)

/**
 * A single bar per month, each as a share of that month's own budget, with a dashed 100% line -
 * 02-features.md F10 ("Previous month"). [PercentBar.isInProgress] draws in the distinct
 * in-progress colour instead of its status colour, since an unfinished month hasn't "passed" or
 * "failed" yet.
 */
@Composable
fun PercentBarChart(bars: List<PercentBar>, modifier: Modifier = Modifier) {
    if (bars.isEmpty()) return
    val trackGridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val valueColor = MaterialTheme.colorScheme.onSurface
    val textMeasurer = rememberTextMeasurer()
    val axisLabelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val monthLabelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val valueLabelStyle = MaterialTheme.typography.labelMedium.copy(color = valueColor, fontWeight = FontWeight.Bold)

    Canvas(modifier = modifier.fillMaxWidth().height(220.dp)) {
        val axisMax = maxOf(1f, bars.maxOf { it.percent }) * 1.15f
        val axisLabelWidth = 36.dp.toPx()
        val bottomLabelHeight = 20.dp.toPx()
        val topPadding = 24.dp.toPx()
        val chartTop = topPadding
        val chartBottom = size.height - bottomLabelHeight
        val chartHeight = chartBottom - chartTop
        val chartLeft = axisLabelWidth

        listOf(0f, 0.5f, 1f).forEach { fraction ->
            val y = chartBottom - (fraction / axisMax) * chartHeight
            drawLine(trackGridColor, Offset(chartLeft, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            val text = textMeasurer.measure("${(fraction * 100).roundToInt()}%", axisLabelStyle)
            drawText(text, topLeft = Offset(chartLeft - text.size.width - 4.dp.toPx(), y - text.size.height / 2f))
        }
        val y100 = chartBottom - (1f / axisMax) * chartHeight
        drawLine(
            color = Color.Black.copy(alpha = 0.6f),
            start = Offset(chartLeft, y100),
            end = Offset(size.width, y100),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
        )

        val slotWidth = (size.width - chartLeft) / bars.size
        val barWidth = slotWidth * 0.5f
        bars.forEachIndexed { index, bar ->
            val barColor = if (bar.isInProgress) StatusColors.inProgress else bar.status.color()
            val barTop = chartBottom - (bar.percent / axisMax).coerceIn(0f, 1f) * chartHeight
            val barLeft = chartLeft + index * slotWidth + (slotWidth - barWidth) / 2f
            drawRoundRect(
                color = barColor,
                topLeft = Offset(barLeft, barTop),
                size = Size(barWidth, chartBottom - barTop),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
            bar.valueLabel?.let { label ->
                val measured = textMeasurer.measure(label, valueLabelStyle)
                drawText(
                    measured,
                    topLeft = Offset(barLeft + barWidth / 2f - measured.size.width / 2f, barTop - measured.size.height - 2.dp.toPx())
                )
            }
            val monthLabel = textMeasurer.measure(bar.label, monthLabelStyle)
            drawText(
                monthLabel,
                topLeft = Offset(barLeft + barWidth / 2f - monthLabel.size.width / 2f, chartBottom + 4.dp.toPx())
            )
        }
    }
}

/** One month's pair of bars for [GroupedBarChart] - Trends' "Historic" tab. Budget and spent are
 *  whole rupees, not paise - this chart only ever shows rounded amounts. */
data class GroupedBar(
    val label: String,
    val budget: Long,
    val spent: Long,
    val status: BudgetStatus,
    val isInProgress: Boolean,
    val valueLabel: String?
)

/**
 * A budget bar and a spent bar per month - 02-features.md F10 ("Historic"). The axis scales to
 * whatever range of amounts is shown, rounded up to a tidy number, rather than a fixed 0-100%
 * scale like [PercentBarChart].
 */
@Composable
fun GroupedBarChart(bars: List<GroupedBar>, modifier: Modifier = Modifier) {
    if (bars.isEmpty()) return
    val budgetColor = MaterialTheme.colorScheme.surfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val valueColor = MaterialTheme.colorScheme.onSurface
    val textMeasurer = rememberTextMeasurer()
    val axisLabelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val monthLabelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val valueLabelStyle = MaterialTheme.typography.labelMedium.copy(color = valueColor, fontWeight = FontWeight.Bold)

    Canvas(modifier = modifier.fillMaxWidth().height(240.dp)) {
        val axisMax = niceAxisMax(bars.maxOf { maxOf(it.budget, it.spent) })
        val axisLabelWidth = 40.dp.toPx()
        val bottomLabelHeight = 20.dp.toPx()
        val topPadding = 24.dp.toPx()
        val chartTop = topPadding
        val chartBottom = size.height - bottomLabelHeight
        val chartHeight = chartBottom - chartTop
        val chartLeft = axisLabelWidth

        listOf(0f, 1 / 3f, 2 / 3f, 1f).forEach { fraction ->
            val y = chartBottom - fraction * chartHeight
            drawLine(gridColor, Offset(chartLeft, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            val text = textMeasurer.measure(formatThousands((axisMax * fraction).roundToLong()), axisLabelStyle)
            drawText(text, topLeft = Offset(chartLeft - text.size.width - 4.dp.toPx(), y - text.size.height / 2f))
        }

        val slotWidth = (size.width - chartLeft) / bars.size
        val pairWidth = slotWidth * 0.6f
        val barWidth = pairWidth / 2f
        bars.forEachIndexed { index, bar ->
            val slotLeft = chartLeft + index * slotWidth + (slotWidth - pairWidth) / 2f
            val budgetTop = chartBottom - (bar.budget.toFloat() / axisMax).coerceIn(0f, 1f) * chartHeight
            drawRoundRect(
                color = budgetColor,
                topLeft = Offset(slotLeft, budgetTop),
                size = Size(barWidth, chartBottom - budgetTop),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
            val spentColor = if (bar.isInProgress) StatusColors.inProgress else bar.status.color()
            val spentTop = chartBottom - (bar.spent.toFloat() / axisMax).coerceIn(0f, 1f) * chartHeight
            val spentLeft = slotLeft + barWidth
            drawRoundRect(
                color = spentColor,
                topLeft = Offset(spentLeft, spentTop),
                size = Size(barWidth, chartBottom - spentTop),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
            bar.valueLabel?.let { label ->
                val measured = textMeasurer.measure(label, valueLabelStyle)
                drawText(
                    measured,
                    topLeft = Offset(spentLeft + barWidth / 2f - measured.size.width / 2f, spentTop - measured.size.height - 2.dp.toPx())
                )
            }
            val monthLabel = textMeasurer.measure(bar.label, monthLabelStyle)
            drawText(
                monthLabel,
                topLeft = Offset(slotLeft + pairWidth / 2f - monthLabel.size.width / 2f, chartBottom + 4.dp.toPx())
            )
        }
    }
}

private val NICE_STEPS = listOf(1.0, 1.2, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0, 6.0, 8.0, 10.0)

/** Rounds up to a tidy axis maximum, e.g. 53,600 -> 60,000 - a "nice number" scale rather than
 *  the exact data max, so gridlines land on round values. */
private fun niceAxisMax(maxValue: Long): Float {
    if (maxValue <= 0) return 100f
    val magnitude = Math.pow(10.0, floor(log10(maxValue.toDouble()))).coerceAtLeast(1.0)
    val normalized = maxValue / magnitude
    val step = NICE_STEPS.firstOrNull { it >= normalized } ?: 10.0
    return (step * magnitude).toFloat()
}

private fun formatThousands(value: Long): String {
    val thousands = value / 1000.0
    return if (thousands == floor(thousands)) "${thousands.toLong()}k" else "%.1fk".format(thousands)
}
