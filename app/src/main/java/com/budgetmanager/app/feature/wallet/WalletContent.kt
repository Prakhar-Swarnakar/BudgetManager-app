package com.budgetmanager.app.feature.wallet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.core.designsystem.components.AmountField
import com.budgetmanager.app.core.designsystem.components.ConfirmDialog
import com.budgetmanager.app.core.designsystem.components.EmptyState
import com.budgetmanager.app.feature.transaction.components.DateField
import com.budgetmanager.app.ui.theme.StatusColors

@Composable
fun WalletContent(
    state: WalletUiState,
    onAddClick: () -> Unit,
    onSpendClick: (WalletEntryUi.Spend) -> Unit,
    onDeleteTopUp: (Long) -> Unit,
    onDeleteConfirmed: () -> Unit,
    onDeleteCancelled: () -> Unit,
    onSheetAmountChanged: (String) -> Unit,
    onSheetDateChanged: (java.time.LocalDate) -> Unit,
    onSheetNoteChanged: (String) -> Unit,
    onSheetSaved: () -> Unit,
    onSheetDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp // clear of the FAB
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "balance") { BalanceCard(state) }
            if (state.entries.isEmpty() && !state.isLoading) {
                item(key = "empty") {
                    Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                        EmptyState(
                            icon = Icons.Default.AccountBalanceWallet,
                            message = "No cash activity yet. Tap + when you withdraw or receive cash; " +
                                "spends paid with Cash appear here on their own."
                        )
                    }
                }
            }
            items(state.entries, key = { it.key }) { entry ->
                when (entry) {
                    is WalletEntryUi.TopUp -> TopUpRow(entry, onDelete = { onDeleteTopUp(entry.id) })
                    is WalletEntryUi.Spend -> SpendRow(entry, onClick = { onSpendClick(entry) })
                }
            }
        }

        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add cash")
        }
    }

    state.sheet?.let { sheet ->
        AddCashSheet(
            state = sheet,
            onAmountChanged = onSheetAmountChanged,
            onDateChanged = onSheetDateChanged,
            onNoteChanged = onSheetNoteChanged,
            onSave = onSheetSaved,
            onDismiss = onSheetDismissed
        )
    }

    if (state.pendingDeleteTopUpId != null) {
        ConfirmDialog(
            title = "Delete this cash entry?",
            message = "Your cash balance drops by this amount. This can't be undone.",
            onConfirm = onDeleteConfirmed,
            onDismiss = onDeleteCancelled
        )
    }
}

@Composable
private fun BalanceCard(state: WalletUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Cash on hand",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                state.balanceText,
                style = MaterialTheme.typography.headlineMedium,
                color = if (state.isNegative) StatusColors.overBudget else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (state.isNegative) {
                Text(
                    "This usually means a top-up wasn't logged, or a non-cash spend got tagged as Cash by mistake.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusColors.overBudget,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun TopUpRow(entry: WalletEntryUi.TopUp, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    entry.dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Text(entry.amountText, style = MaterialTheme.typography.titleMedium, color = StatusColors.accepted)
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Close, contentDescription = "Delete this cash entry")
            }
        }
    }
}

@Composable
private fun SpendRow(entry: WalletEntryUi.Spend, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(entry.categoryEmoji, style = MaterialTheme.typography.headlineSmall)
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(entry.title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${entry.dateText} \u00b7 ${entry.categoryName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(entry.amountText, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCashSheet(
    state: AddCashSheetUiState,
    onAmountChanged: (String) -> Unit,
    onDateChanged: (java.time.LocalDate) -> Unit,
    onNoteChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Add cash", style = MaterialTheme.typography.titleMedium)

            AmountField(
                value = state.amountInput,
                onValueChange = onAmountChanged,
                isError = state.amountError != null,
                supportingText = state.amountError,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            )
            Spacer(Modifier.height(12.dp))
            DateField(date = state.date, onDateChanged = onDateChanged)
            OutlinedTextField(
                value = state.note,
                onValueChange = onNoteChanged,
                label = { Text("Note (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(onClick = onSave, modifier = Modifier.weight(1f)) { Text("Save") }
            }
        }
    }
}
