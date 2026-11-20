package com.budgetmanager.app.feature.transaction

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetmanager.app.navigation.Destination

@Composable
fun AddTransactionScreen(
    messageId: Long?,
    transactionId: Long?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    prefillAmountPaise: Long? = null,
    prefillTitle: String? = null,
    prefillDateEpochDay: Long? = null,
    prefillBannerText: String? = null,
    // Keyed the same way the top-bar Save button's own hiltViewModel() call in AppNavigation is
    // (see Destination.AddTransaction.viewModelKey()), so both resolve the same cached instance.
    viewModel: AddTransactionViewModel = hiltViewModel(
        key = Destination.AddTransaction(
            messageId, transactionId, prefillAmountPaise, prefillTitle, prefillDateEpochDay, prefillBannerText
        ).viewModelKey()
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(messageId, transactionId) {
        viewModel.load(messageId, transactionId, prefillAmountPaise, prefillTitle, prefillDateEpochDay, prefillBannerText)
    }

    LaunchedEffect(state.saved) {
        if (state.saved) {
            onDone()
            viewModel.onSavedHandled()
        }
    }

    AddTransactionContent(
        state = state,
        onAmountChanged = viewModel::onAmountChanged,
        onMerchantChanged = viewModel::onMerchantChanged,
        onAddToRuleToggled = viewModel::onAddToRuleToggled,
        onCategoryKeywordChanged = viewModel::onCategoryKeywordChanged,
        onTaxonomySelected = viewModel::onTaxonomySelected,
        onAddTaxonomyToRuleToggled = viewModel::onAddTaxonomyToRuleToggled,
        onTaxonomyKeywordChanged = viewModel::onTaxonomyKeywordChanged,
        onNoteChanged = viewModel::onNoteChanged,
        onDateChanged = viewModel::onDateChanged,
        onCategorySelected = viewModel::onCategorySelected,
        onSplitToggled = viewModel::onSplitToggled,
        onAddSplitFriend = viewModel::onAddSplitFriend,
        onRemoveSplitFriend = viewModel::onRemoveSplitFriend,
        onSplitFriendSettledToggled = viewModel::onSplitFriendSettledToggled,
        onSplitFriendAmountChanged = viewModel::onSplitFriendAmountChanged,
        modifier = modifier
    )
}
