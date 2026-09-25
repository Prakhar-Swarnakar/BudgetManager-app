package com.budgetmanager.app.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetmanager.app.core.model.MonthKey

@Composable
fun HomeScreen(
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToCategoryDetail: (categoryId: Long, categoryName: String, monthKey: MonthKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    HomeContent(
        state = state,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onCardClick = { categoryId ->
            state.cards.firstOrNull { it.categoryId == categoryId }?.let { card ->
                onNavigateToCategoryDetail(categoryId, card.name, state.monthKey)
            }
        },
        onAddClick = onNavigateToAddTransaction,
        modifier = modifier
    )
}
