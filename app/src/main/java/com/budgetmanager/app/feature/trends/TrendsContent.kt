package com.budgetmanager.app.feature.trends

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.core.designsystem.components.DonutChart
import com.budgetmanager.app.core.designsystem.components.EmptyState
import com.budgetmanager.app.core.designsystem.components.color
import com.budgetmanager.app.core.model.BudgetStatus
import com.budgetmanager.app.ui.theme.StatusColors

@Composable
fun TrendsThisMonthContent(state: TrendsUiState, modifier: Modifier = Modifier) {
    if (state.isLoading) return

    if (!state.hasBudget) {
        EmptyState(
            icon = Icons.AutoMirrored.Filled.ShowChart,
            message = "No budget set for ${state.monthLabel} yet. Set it up from Monthly budget.",
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "${state.monthLabel} budget allocation",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Each slice is a category's share of the ${state.totalBudgetText} budget. " +
                        "The coloured part is how much is used.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                DonutChart(
                    slices = state.slices,
                    centerPercentText = state.overallPercentText,
                    centerSubText = "of budget used",
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .fillMaxWidth(0.7f)
                        .align(Alignment.CenterHorizontally)
                )
                DonutLegend(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }

        Text(
            "By category",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                state.rows.forEachIndexed { index, row ->
                    TrendsCategoryRow(row)
                    if (index != state.rows.lastIndex) HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun TrendsCategoryRow(row: TrendsCategoryRowUi) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(row.emoji, style = MaterialTheme.typography.headlineSmall)
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(row.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                row.allocatedText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(row.usedText, style = MaterialTheme.typography.titleMedium)
            val statusColor = if (row.status == BudgetStatus.UNDER_BUDGET && !row.isNotBudgeted) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                row.status.color()
            }
            Text(row.statusText, style = MaterialTheme.typography.labelMedium, color = statusColor)
        }
    }
}

@Composable
private fun DonutLegend(modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        LegendSwatch("Used", StatusColors.underBudget)
        LegendSwatch("80%+ used", StatusColors.warning)
        LegendSwatch("Over budget", StatusColors.overBudget)
        LegendSwatch("Left", MaterialTheme.colorScheme.surfaceVariant, isLast = true)
    }
}

@Composable
private fun LegendSwatch(label: String, color: Color, isLast: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = if (isLast) 0.dp else 12.dp)
    ) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}
