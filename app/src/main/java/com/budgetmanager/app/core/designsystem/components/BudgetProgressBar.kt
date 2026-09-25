package com.budgetmanager.app.core.designsystem.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.core.model.BudgetStatus
import com.budgetmanager.app.ui.theme.StatusColors

/** Colour comes from [BudgetStatus]: blue below 80%, amber from 80%, red over budget - one
 *  place, reused by the bar, the status word, and (in the domain layer) the alert check, per
 *  05-budget-rules.md. */
fun BudgetStatus.color() = when (this) {
    BudgetStatus.UNDER_BUDGET -> StatusColors.underBudget
    BudgetStatus.WARNING -> StatusColors.warning
    BudgetStatus.OVER_BUDGET -> StatusColors.overBudget
}

@Composable
fun BudgetProgressBar(
    percentUsed: Double?,
    status: BudgetStatus,
    modifier: Modifier = Modifier
) {
    val progress = (percentUsed ?: 1.0).toFloat().coerceIn(0f, 1f)
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier.fillMaxWidth(),
        color = status.color(),
        trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

/** Status as an icon plus a word, so colour is never the only signal (05-budget-rules.md). */
@Composable
fun BudgetStatusLabel(status: BudgetStatus, isNotBudgeted: Boolean, modifier: Modifier = Modifier) {
    val (icon, label) = when {
        isNotBudgeted -> Icons.Default.PriorityHigh to "Not budgeted"
        status == BudgetStatus.OVER_BUDGET -> Icons.Default.PriorityHigh to "Over budget"
        status == BudgetStatus.WARNING -> Icons.Default.Warning to "Near budget"
        else -> Icons.Default.Check to "Under budget"
    }
    val color = status.color()
    Row(modifier = modifier) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.padding(end = 4.dp))
        CompositionLocalProvider(LocalContentColor provides color) {
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
