package com.budgetmanager.app.feature.categoryrules

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CategoryRulesScreen(
    modifier: Modifier = Modifier,
    viewModel: CategoryRulesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CategoryRulesContent(
        state = state,
        onAddClicked = viewModel::onAddClicked,
        onRuleClicked = viewModel::onRuleClicked,
        onDeleteRequested = viewModel::onDeleteRequested,
        onKeywordChanged = viewModel::onKeywordChanged,
        onCategorySelected = viewModel::onCategorySelected,
        onSaveEdit = viewModel::onSaveEdit,
        onEditDismissed = viewModel::onEditDismissed,
        onConfirmDelete = viewModel::onConfirmDelete,
        onCancelDelete = viewModel::onCancelDelete,
        modifier = modifier
    )
}
