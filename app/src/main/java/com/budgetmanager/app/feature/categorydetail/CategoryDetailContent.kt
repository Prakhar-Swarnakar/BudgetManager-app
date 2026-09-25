package com.budgetmanager.app.feature.categorydetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.core.designsystem.components.BudgetProgressBar
import com.budgetmanager.app.core.designsystem.components.BudgetStatusLabel
import com.budgetmanager.app.core.designsystem.components.ConfirmDialog
import com.budgetmanager.app.core.designsystem.components.EmptyState
import com.budgetmanager.app.core.designsystem.components.MonthSelector
import com.budgetmanager.app.feature.categorydetail.components.TransactionRow

@Composable
fun CategoryDetailContent(
    state: CategoryDetailUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    onDeleteSwiped: (Long) -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        MonthSelector(
            monthKey = state.monthKey,
            onPrevious = onPreviousMonth,
            onNext = onNextMonth,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )

        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(state.categoryEmoji, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "${state.spentText} of ${state.budgetText}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 12.dp).weight(1f)
                    )
                    Text(state.remainingText, style = MaterialTheme.typography.titleMedium)
                }
                BudgetProgressBar(
                    percentUsed = state.percentUsed,
                    status = state.status,
                    modifier = Modifier.padding(top = 8.dp)
                )
                BudgetStatusLabel(
                    status = state.status,
                    isNotBudgeted = state.isNotBudgeted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        if (state.transactions.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Receipt,
                message = "No transactions this month yet."
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.transactions, key = { it.id }) { row ->
                    TransactionRow(
                        row = row,
                        onClick = { onTransactionClick(row.id) },
                        onDeleteSwiped = { onDeleteSwiped(row.id) }
                    )
                }
            }
        }
    }

    if (state.pendingDeleteTransactionId != null) {
        ConfirmDialog(
            title = "Delete transaction?",
            message = "This can't be undone.",
            onConfirm = onConfirmDelete,
            onDismiss = onCancelDelete
        )
    }
}
