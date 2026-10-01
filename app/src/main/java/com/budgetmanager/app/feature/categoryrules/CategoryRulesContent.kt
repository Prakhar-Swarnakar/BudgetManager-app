package com.budgetmanager.app.feature.categoryrules

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.core.designsystem.components.CategoryChipGrid
import com.budgetmanager.app.core.designsystem.components.ConfirmDialog
import com.budgetmanager.app.core.designsystem.components.EmptyState
import com.budgetmanager.app.core.model.Category

@Composable
fun CategoryRulesContent(
    state: CategoryRulesUiState,
    onAddClicked: () -> Unit,
    onRuleClicked: (CategoryRuleUi) -> Unit,
    onDeleteRequested: (String) -> Unit,
    onKeywordChanged: (String) -> Unit,
    onCategorySelected: (Long) -> Unit,
    onSaveEdit: () -> Unit,
    onEditDismissed: () -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (!state.isLoading && state.rules.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Label,
                message = "No rules yet. A rule maps a word in a message to a category, so " +
                    "matching messages suggest it automatically."
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.rules, key = { it.keyword }) { rule ->
                    RuleRow(rule, onClick = { onRuleClicked(rule) }, onDelete = { onDeleteRequested(rule.keyword) })
                    HorizontalDivider()
                }
                item { Spacer(Modifier.height(72.dp)) } // keeps the last row clear of the FAB
            }
        }
        FloatingActionButton(
            onClick = onAddClicked,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "New rule")
        }
    }

    state.editing?.let { editing ->
        EditRuleSheet(
            editing = editing,
            categories = state.categories,
            onKeywordChanged = onKeywordChanged,
            onCategorySelected = onCategorySelected,
            onSave = onSaveEdit,
            onDismiss = onEditDismissed
        )
    }

    state.pendingDeleteKeyword?.let { keyword ->
        ConfirmDialog(
            title = "Delete rule?",
            message = "Messages containing \"$keyword\" will no longer get a category suggested automatically.",
            confirmLabel = "Delete",
            onConfirm = onConfirmDelete,
            onDismiss = onCancelDelete
        )
    }
}

@Composable
private fun RuleRow(rule: CategoryRuleUi, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("\"${rule.keyword}\"", style = MaterialTheme.typography.bodyLarge)
            Text(
                "${rule.categoryEmoji} ${rule.categoryName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete rule")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditRuleSheet(
    editing: EditingRuleUi,
    categories: List<Category>,
    onKeywordChanged: (String) -> Unit,
    onCategorySelected: (Long) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(if (editing.isNew) "New rule" else "Edit rule", style = MaterialTheme.typography.titleMedium)

            if (editing.isNew) {
                OutlinedTextField(
                    value = editing.keyword,
                    onValueChange = onKeywordChanged,
                    label = { Text("Word to match") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
            } else {
                Text(
                    "\"${editing.keyword}\"",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Text(
                "Category",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            CategoryChipGrid(
                categories = categories,
                selectedCategoryId = editing.selectedCategoryId,
                onSelect = onCategorySelected
            )

            Button(
                onClick = onSave,
                enabled = editing.keyword.isNotBlank() && editing.selectedCategoryId != null,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text("Save")
            }
        }
    }
}
