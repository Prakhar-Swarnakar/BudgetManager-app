package com.budgetmanager.app.feature.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.core.designsystem.components.AmountField
import com.budgetmanager.app.core.designsystem.components.CategoryChipGrid
import com.budgetmanager.app.feature.transaction.components.DateField
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.OutlinedTextField

@Composable
fun AddTransactionContent(
    state: AddTransactionUiState,
    onAmountChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onDateChanged: (java.time.LocalDate) -> Unit,
    onCategorySelected: (Long) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Explicitly dismiss the keyboard/clear focus as part of the click itself, rather than
    // relying on the system's own tap-to-dismiss handling - on some devices the first tap
    // outside a focused field is consumed just to close the keyboard, so a button underneath
    // needs an extra tap (or two) before its own click actually registers.
    fun dismissKeyboardThen(action: () -> Unit): () -> Unit = {
        keyboardController?.hide()
        focusManager.clearFocus()
        action()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        state.smsBannerText?.let { text ->
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Text(
                    text,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        AmountField(
            value = state.amountInput,
            onValueChange = onAmountChanged,
            isError = state.amountError != null,
            supportingText = state.amountError,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))
        Text("Category", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        CategoryChipGrid(
            categories = state.categories,
            selectedCategoryId = state.selectedCategoryId,
            suggestedCategoryId = state.suggestedCategoryId,
            onSelect = onCategorySelected
        )
        state.categoryError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(Modifier.height(20.dp))
        DateField(date = state.date, onDateChanged = onDateChanged)

        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = state.note,
            onValueChange = onNoteChanged,
            label = { Text("Note (optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = dismissKeyboardThen(onCancel), modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            Button(
                onClick = dismissKeyboardThen(onSave),
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (state.isEditMode) "Save changes" else "Save")
            }
        }
    }
}
