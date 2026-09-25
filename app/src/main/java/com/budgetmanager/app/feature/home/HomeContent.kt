package com.budgetmanager.app.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.core.designsystem.components.BudgetProgressBar
import com.budgetmanager.app.core.designsystem.components.BudgetStatusLabel
import com.budgetmanager.app.core.designsystem.components.EmptyState
import com.budgetmanager.app.core.designsystem.components.MonthSelector

@Composable
fun HomeContent(
    state: HomeUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCardClick: (Long) -> Unit,
    onAddClick: () -> Unit,
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

            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Spent ${state.totalSpentText} of ${state.totalBudgetText}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(state.totalRemainingText, style = MaterialTheme.typography.titleMedium)
                    }
                    BudgetProgressBar(
                        percentUsed = state.totalPercentUsed,
                        status = state.totalStatus,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            if (state.reviewCount > 0) {
                Text(
                    "${state.reviewCount} messages to review",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            if (state.cards.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Home,
                    message = "No categories yet. Set up your budget from the side panel."
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    state.cards.forEach { card ->
                        HomeCategoryCard(card = card, onClick = { onCardClick(card.categoryId) })
                    }
                    Spacer(Modifier.height(80.dp)) // keeps the last card clear of the FAB
                }
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

@Composable
private fun HomeCategoryCard(card: HomeCategoryCardUi, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(card.emoji, style = MaterialTheme.typography.headlineSmall)
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(card.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${card.spentText} spent",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(card.remainingText, style = MaterialTheme.typography.titleMedium)
            }
            BudgetProgressBar(
                percentUsed = card.percentUsed,
                status = card.status,
                modifier = Modifier.padding(top = 8.dp)
            )
            BudgetStatusLabel(
                status = card.status,
                isNotBudgeted = card.isNotBudgeted,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
