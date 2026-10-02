package com.budgetmanager.app.feature.taxonomyrules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.core.designsystem.components.ConfirmDialog
import com.budgetmanager.app.core.model.TaxonomyType

@Composable
fun TaxonomyRulesContent(
    state: TaxonomyRulesUiState,
    onAddClicked: (TaxonomyType) -> Unit,
    onRuleClicked: (keyword: String, taxonomy: TaxonomyType) -> Unit,
    onDeleteRequested: (String) -> Unit,
    onKeywordChanged: (String) -> Unit,
    onTaxonomySelected: (TaxonomyType) -> Unit,
    onSaveEdit: () -> Unit,
    onEditDismissed: () -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Unlike Category rules, there's no "no groups" empty state here - the taxonomy set is
    // fixed at 8 values, so once loaded there are always exactly 8 groups.
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(state.groups, key = { it.taxonomy }) { group ->
            TaxonomyGroupSection(
                group = group,
                onChipClick = { keyword -> onRuleClicked(keyword, group.taxonomy) },
                onChipDelete = onDeleteRequested,
                onAddClick = { onAddClicked(group.taxonomy) }
            )
            HorizontalDivider()
        }
    }

    state.editing?.let { editing ->
        EditTaxonomyRuleSheet(
            editing = editing,
            onKeywordChanged = onKeywordChanged,
            onTaxonomySelected = onTaxonomySelected,
            onSave = onSaveEdit,
            onDismiss = onEditDismissed
        )
    }

    state.pendingDeleteKeyword?.let { keyword ->
        ConfirmDialog(
            title = "Delete rule?",
            message = "Messages containing \"$keyword\" will no longer get a payment method suggested automatically.",
            confirmLabel = "Delete",
            onConfirm = onConfirmDelete,
            onDismiss = onCancelDelete
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TaxonomyGroupSection(
    group: TaxonomyGroupUi,
    onChipClick: (String) -> Unit,
    onChipDelete: (String) -> Unit,
    onAddClick: () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(group.taxonomy.label, style = MaterialTheme.typography.titleSmall)
            Text(
                wordCountLabel(group.keywords.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            group.keywords.forEach { keyword ->
                InputChip(
                    selected = false,
                    onClick = { onChipClick(keyword) },
                    label = { Text(keyword) },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Delete \"$keyword\"",
                            modifier = Modifier
                                .size(InputChipDefaults.IconSize)
                                // Its own tap target, separate from the chip's onClick above -
                                // otherwise tapping the x would open the edit sheet instead of
                                // deleting.
                                .clickable(onClick = { onChipDelete(keyword) })
                        )
                    }
                )
            }
            AssistChip(onClick = onAddClick, label = { Text("+ add") })
        }
    }
}

private fun wordCountLabel(count: Int): String = when (count) {
    0 -> "No words"
    1 -> "1 word"
    else -> "$count words"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTaxonomyRuleSheet(
    editing: EditingTaxonomyRuleUi,
    onKeywordChanged: (String) -> Unit,
    onTaxonomySelected: (TaxonomyType) -> Unit,
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
                "Payment method",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            TaxonomyChipGrid(
                selectedTaxonomy = editing.selectedTaxonomy,
                onSelect = onTaxonomySelected
            )

            Button(
                onClick = onSave,
                enabled = editing.keyword.isNotBlank() && editing.selectedTaxonomy != null,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text("Save")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaxonomyChipGrid(selectedTaxonomy: TaxonomyType?, onSelect: (TaxonomyType) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TaxonomyType.entries.forEach { taxonomy ->
            FilterChip(
                selected = taxonomy == selectedTaxonomy,
                onClick = { onSelect(taxonomy) },
                label = { Text(taxonomy.label) }
            )
        }
    }
}
