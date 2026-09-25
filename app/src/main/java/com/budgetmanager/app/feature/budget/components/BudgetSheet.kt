package com.budgetmanager.app.feature.budget.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.core.designsystem.components.AmountField
import com.budgetmanager.app.core.designsystem.components.EmojiIconBox
import com.budgetmanager.app.feature.budget.BudgetSheetMode
import com.budgetmanager.app.feature.budget.BudgetSheetUiState

/** One sheet for both jobs, per 08-pages-and-navigation.md: editing an existing category's
 *  amount (tap a row) only shows the amount field; creating a new category (the + button) adds
 *  the emoji and name fields above it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSheet(
    state: BudgetSheetUiState,
    onAmountChanged: (String) -> Unit,
    onNameChanged: (String) -> Unit,
    onEmojiChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val isNewCategory = state.mode is BudgetSheetMode.NewCategory

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                if (isNewCategory) "New category" else "Edit amount",
                style = MaterialTheme.typography.titleMedium
            )

            if (isNewCategory) {
                EmojiIconBox(
                    emoji = state.emoji,
                    onEmojiChanged = onEmojiChanged,
                    isError = state.emojiError != null,
                    modifier = Modifier.padding(top = 16.dp)
                )
                state.emojiError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
                }

                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChanged,
                    label = { Text("Name") },
                    isError = state.nameError != null,
                    supportingText = state.nameError?.let { error -> { Text(error) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                )
            }

            AmountField(
                value = state.amountInput,
                onValueChange = onAmountChanged,
                isError = state.amountError != null,
                supportingText = state.amountError,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(onClick = onSave, modifier = Modifier.weight(1f)) {
                    Text("Save")
                }
            }
        }
    }
}
