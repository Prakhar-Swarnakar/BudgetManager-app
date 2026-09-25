package com.budgetmanager.app.feature.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.core.designsystem.components.EmptyState
import com.budgetmanager.app.core.designsystem.components.MonthSelector
import com.budgetmanager.app.feature.budget.components.BudgetCategoryList
import com.budgetmanager.app.feature.budget.components.BudgetSheet

@Composable
fun MonthlyBudgetContent(
    state: MonthlyBudgetUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onRowClick: (Long) -> Unit,
    onReorder: (List<Long>) -> Unit,
    onSheetNameChanged: (String) -> Unit,
    onSheetEmojiChanged: (String) -> Unit,
    onSheetAmountChanged: (String) -> Unit,
    onSheetSaved: () -> Unit,
    onSheetDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        MonthSelector(
            monthKey = state.monthKey,
            onPrevious = onPreviousMonth,
            onNext = onNextMonth,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )

        if (state.copiedFromPreviousMonth) {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text(
                    "Copied from last month",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (state.rows.isEmpty()) {
            EmptyState(
                icon = Icons.Default.AccountBalanceWallet,
                message = "No categories yet. Tap + to add one."
            )
        } else {
            // Scrollable: with 9+ starter categories the list can run past one screen. Only the
            // populated branch scrolls, nested inside the non-scrolling outer Column, so
            // EmptyState's fillMaxSize() above still gets a bounded height (an unbounded
            // scrollable parent would crash it).
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                BudgetCategoryList(
                    rows = state.rows,
                    onRowClick = onRowClick,
                    onReorder = onReorder
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total", style = MaterialTheme.typography.titleMedium)
                    Text(state.totalText, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }

    state.sheet?.let { sheet ->
        BudgetSheet(
            state = sheet,
            onNameChanged = onSheetNameChanged,
            onEmojiChanged = onSheetEmojiChanged,
            onAmountChanged = onSheetAmountChanged,
            onSave = onSheetSaved,
            onDismiss = onSheetDismissed
        )
    }
}
