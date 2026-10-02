package com.budgetmanager.app.feature.taxonomyrules

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun TaxonomyRulesScreen(
    modifier: Modifier = Modifier,
    viewModel: TaxonomyRulesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TaxonomyRulesContent(
        state = state,
        onAddClicked = viewModel::onAddClicked,
        onRuleClicked = viewModel::onRuleClicked,
        onDeleteRequested = viewModel::onDeleteRequested,
        onKeywordChanged = viewModel::onKeywordChanged,
        onTaxonomySelected = viewModel::onTaxonomySelected,
        onSaveEdit = viewModel::onSaveEdit,
        onEditDismissed = viewModel::onEditDismissed,
        onConfirmDelete = viewModel::onConfirmDelete,
        onCancelDelete = viewModel::onCancelDelete,
        modifier = modifier
    )
}
