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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.core.model.BudgetStatus

/**
 * A budget bar with an optional "today" mark: a thin vertical line at [paceMark] (0..1, how far
 * through the month it is). Fill length is [percentUsed], capped at full; colour comes from
 * [status]. Home passes a pace-based status (blue / amber / red), see HomePace.kt.
 */
@Composable
fun PaceProgressBar(
    percentUsed: Double?,
    status: BudgetStatus,
    paceMark: Double?,
    modifier: Modifier = Modifier,
    thickness: Dp = 6.dp
) {
    val fill = (percentUsed ?: 1.0).toFloat().coerceIn(0f, 1f)
    val fillColor = status.color()
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val markColor = MaterialTheme.colorScheme.onSurface
    Canvas(modifier = modifier.fillMaxWidth().height(thickness)) {
        val corner = CornerRadius(size.height / 2)
        drawRoundRect(trackColor, size = size, cornerRadius = corner)
        if (fill > 0f) {
            drawRoundRect(fillColor, size = Size(size.width * fill, size.height), cornerRadius = corner)
        }
        if (paceMark != null) {
            val x = size.width * paceMark.toFloat().coerceIn(0f, 1f)
            val overhang = 3.dp.toPx()
            drawLine(
                color = markColor,
                start = Offset(x, -overhang),
                end = Offset(x, size.height + overhang),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}
