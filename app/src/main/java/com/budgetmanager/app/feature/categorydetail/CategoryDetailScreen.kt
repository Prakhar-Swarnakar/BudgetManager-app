package com.budgetmanager.app.feature.categorydetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetmanager.app.core.model.MonthKey

@Composable
fun CategoryDetailScreen(
    categoryId: Long,
    monthKey: MonthKey,
    onNavigateToEditTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier,
    // A distinct key per categoryId: without it, every category's detail page would share one
    // Hilt-cached ViewModel instance (there's no per-NavEntry ViewModelStore scoping wired up
    // for Navigation 3 here - see the same note on AddTransactionScreen).
    viewModel: CategoryDetailViewModel = hiltViewModel(key = "CategoryDetail:$categoryId")
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(categoryId, monthKey) {
        viewModel.load(categoryId, monthKey)
    }

    LaunchedEffect(state.editTransactionId) {
        val id = state.editTransactionId ?: return@LaunchedEffect
        onNavigateToEditTransaction(id)
        viewModel.onEditNavigationHandled()
    }

    CategoryDetailContent(
        state = state,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onTransactionClick = viewModel::onTransactionClicked,
        onDeleteSwiped = viewModel::onDeleteSwiped,
        onUndoDelete = viewModel::onUndoDelete,
        onUndoDismissed = viewModel::onUndoDismissed,
        modifier = modifier
    )
}
