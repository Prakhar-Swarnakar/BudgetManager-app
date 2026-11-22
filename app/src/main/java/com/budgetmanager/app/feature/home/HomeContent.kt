package com.budgetmanager.app.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.core.designsystem.components.EmptyState
import com.budgetmanager.app.core.designsystem.components.MonthSelector
import com.budgetmanager.app.core.designsystem.components.PaceProgressBar
import com.budgetmanager.app.core.designsystem.components.color
import com.budgetmanager.app.core.model.BudgetStatus
import com.budgetmanager.app.ui.theme.StatusColors

@Composable
fun HomeContent(
    state: HomeUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCardClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onReviewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            MonthSelector(
                monthKey = state.monthKey,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth,
                canGoNext = state.canGoNext,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            // Everything below the month selector scrolls, so every category stays reachable
            // even though the summary now takes more room (04-home-redesign.html).
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                SpentSummary(state)
                CashAndOwedStrip(state)
                Text(
                    "Income ${state.totalIncomeText} · Saved ${state.totalSavedText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                if (state.reviewCount > 0) {
                    Text(
                        "${state.reviewCount} message${if (state.reviewCount == 1) "" else "s"} to review",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable(onClick = onReviewClick)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth()
                    )
                }

                if (state.cards.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.Home,
                        message = "No categories yet. Set up your budget from the side panel."
                    )
                } else {
                    Text(
                        "Categories",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
                    )
                    state.cards.forEach { card ->
                        HomeCategoryRow(
                            card = card,
                            paceFraction = state.paceFraction,
                            onClick = { onCardClick(card.categoryId) }
                        )
                    }
                }
                Spacer(Modifier.height(88.dp)) // keeps the last row clear of the FAB
            }
        }

        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add transaction")
        }
    }
}

/** The month's spend as the biggest thing on screen, its bar with the "today" mark, and one
 *  plain sentence about pace next to what is left. */
@Composable
private fun SpentSummary(state: HomeUiState) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            "Spent this month",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(state.totalSpentText, style = MaterialTheme.typography.displayMedium)
            Text(
                "of ${state.totalBudgetText}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )
        }
        PaceProgressBar(
            percentUsed = state.totalPercentUsed,
            status = state.totalStatus,
            paceMark = state.paceFraction,
            thickness = 12.dp,
            modifier = Modifier.padding(top = 8.dp)
        )
        state.paceFraction?.let { TodayLabel(it) }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                state.paceText.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = state.totalStatus.textColor(),
                modifier = Modifier.weight(1f)
            )
            Text(
                state.totalLeftLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = leftColor(state.totalStatus)
            )
        }
    }
}

/** "Today" centred under the mark on the total bar; category bars carry the mark unlabelled. */
@Composable
private fun TodayLabel(fraction: Double) {
    Box(modifier = Modifier.fillMaxWidth().height(18.dp)) {
        Text(
            "Today",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minWidth = 0))
                layout(constraints.maxWidth, placeable.height) {
                    val centred = (constraints.maxWidth * fraction).toInt() - placeable.width / 2
                    placeable.place(centred.coerceIn(0, constraints.maxWidth - placeable.width), 0)
                }
            }
        )
    }
}

/** Avoidable (spend marked at Add Transaction), Cash on hand and Owed to you. */
@Composable
private fun CashAndOwedStrip(state: HomeUiState) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        HorizontalDivider()
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            StripCell(
                "Avoidable", state.avoidableText, state.avoidableShareText ?: "this month", Modifier.weight(1f)
            )
            VerticalDivider(modifier = Modifier.fillMaxHeight())
            StripCell("Cash on hand", state.cashOnHandText, "Wallet", Modifier.weight(1f), startPadding = 12)
            VerticalDivider(modifier = Modifier.fillMaxHeight())
            StripCell("Owed to you", state.totalOwedText, "this month", Modifier.weight(1f), startPadding = 12)
        }
        HorizontalDivider()
    }
}

@Composable
private fun StripCell(
    label: String,
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
    startPadding: Int = 0
) {
    Column(modifier = modifier.padding(start = startPadding.dp, top = 10.dp, bottom = 10.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A flat category row: name alone on the left, spent with "left" under it on the right, and a
 *  bar carrying the unlabelled today mark. */
@Composable
private fun HomeCategoryRow(card: HomeCategoryCardUi, paceFraction: Double?, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(card.emoji, style = MaterialTheme.typography.headlineSmall)
                Text(
                    card.name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 12.dp).weight(1f)
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(card.spentText, style = MaterialTheme.typography.titleMedium)
                    Text(
                        card.leftLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = leftColor(card.status)
                    )
                }
            }
            PaceProgressBar(
                percentUsed = card.percentUsed,
                status = card.status,
                paceMark = paceFraction,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    }
}

/** Over budget is stated in red text as well as on the bar, so colour is not the only signal. */
@Composable
private fun leftColor(status: BudgetStatus) =
    if (status == BudgetStatus.OVER_BUDGET) StatusColors.overBudget
    else MaterialTheme.colorScheme.onSurfaceVariant

@Composable
private fun BudgetStatus.textColor() = when (this) {
    BudgetStatus.UNDER_BUDGET -> MaterialTheme.colorScheme.onSurface
    else -> color()
}
