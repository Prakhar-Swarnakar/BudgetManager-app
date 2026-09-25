package com.budgetmanager.app.feature.categories

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CategoriesScreen(
    modifier: Modifier = Modifier,
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CategoriesContent(
        state = state,
        onRowClick = viewModel::onCategoryClicked,
        onArchive = viewModel::onArchiveSwiped,
        onUnarchive = viewModel::onUnarchiveClicked,
        onReorder = viewModel::onReorder,
        onArchivedSectionToggled = viewModel::onArchivedSectionToggled,
        onSheetNameChanged = viewModel::onSheetNameChanged,
        onSheetEmojiChanged = viewModel::onSheetEmojiChanged,
        onSheetSaved = viewModel::onSheetSaved,
        onSheetDismissed = viewModel::onSheetDismissed,
        modifier = modifier
    )
}
