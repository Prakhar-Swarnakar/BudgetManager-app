package com.budgetmanager.app.feature.taxonomyrules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.budgetmanager.app.core.designsystem.components.ConfirmDialog
import com.budgetmanager.app.core.designsystem.components.RuleEditSheet
import com.budgetmanager.app.core.designsystem.components.RuleGroupUi
import com.budgetmanager.app.core.designsystem.components.RuleGroupsList
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
    val groups = state.groups.map { RuleGroupUi(key = it.taxonomy, label = it.taxonomy.label, keywords = it.keywords) }
    RuleGroupsList(
        groups = groups,
        onChipClick = { keyword, taxonomy -> onRuleClicked(keyword, taxonomy) },
        onChipDelete = onDeleteRequested,
        onAddClick = onAddClicked,
        modifier = modifier
    )

    state.editing?.let { editing ->
        RuleEditSheet(
            isNew = editing.isNew,
            keyword = editing.keyword,
            onKeywordChanged = onKeywordChanged,
            pickerLabel = "Payment method",
            saveEnabled = editing.keyword.isNotBlank() && editing.selectedTaxonomy != null,
            onSave = onSaveEdit,
            onDismiss = onEditDismissed
        ) {
            TaxonomyChipGrid(selectedTaxonomy = editing.selectedTaxonomy, onSelect = onTaxonomySelected)
        }
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
