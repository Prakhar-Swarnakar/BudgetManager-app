package com.budgetmanager.app.feature.categoryrules

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Label
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.budgetmanager.app.core.designsystem.components.CategoryChipGrid
import com.budgetmanager.app.core.designsystem.components.ConfirmDialog
import com.budgetmanager.app.core.designsystem.components.EmptyState
import com.budgetmanager.app.core.designsystem.components.RuleEditSheet
import com.budgetmanager.app.core.designsystem.components.RuleGroupUi
import com.budgetmanager.app.core.designsystem.components.RuleGroupsList

@Composable
fun CategoryRulesContent(
    state: CategoryRulesUiState,
    onAddClicked: (Long) -> Unit,
    onRuleClicked: (keyword: String, categoryId: Long) -> Unit,
    onDeleteRequested: (String) -> Unit,
    onKeywordChanged: (String) -> Unit,
    onCategorySelected: (Long) -> Unit,
    onSaveEdit: () -> Unit,
    onEditDismissed: () -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isLoading && state.groups.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Label,
            message = "No categories yet. Set up your budget from the side panel first.",
            modifier = modifier
        )
    } else {
        val groups = state.groups.map {
            RuleGroupUi(key = it.categoryId, label = "${it.categoryEmoji} ${it.categoryName}", keywords = it.keywords)
        }
        RuleGroupsList(
            groups = groups,
            onChipClick = { keyword, categoryId -> onRuleClicked(keyword, categoryId) },
            onChipDelete = onDeleteRequested,
            onAddClick = onAddClicked,
            modifier = modifier
        )
    }

    state.editing?.let { editing ->
        RuleEditSheet(
            isNew = editing.isNew,
            keyword = editing.keyword,
            onKeywordChanged = onKeywordChanged,
            pickerLabel = "Category",
            saveEnabled = editing.keyword.isNotBlank() && editing.selectedCategoryId != null,
            onSave = onSaveEdit,
            onDismiss = onEditDismissed
        ) {
            CategoryChipGrid(
                categories = state.categories,
                selectedCategoryId = editing.selectedCategoryId,
                onSelect = onCategorySelected
            )
        }
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
