package com.budgetmanager.app.feature.categoryrules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmanager.app.data.repository.CategoryRepository
import com.budgetmanager.app.data.repository.KeywordRuleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryRulesViewModel @Inject constructor(
    private val keywordRuleRepository: KeywordRuleRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val editing = MutableStateFlow<EditingRuleUi?>(null)
    private val pendingDeleteKeyword = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CategoryRulesUiState> = combine(
        keywordRuleRepository.observeAll(),
        categoryRepository.observeActive(),
        editing,
        pendingDeleteKeyword
    ) { rules, categories, editingState, pendingDelete ->
        val keywordsByCategory = rules.groupBy({ it.categoryId }, valueTransform = { it.keyword })
        CategoryRulesUiState(
            isLoading = false,
            // Every active category gets a group, even an empty one - its "+ add" chip is the
            // easiest way to create that category's first rule.
            groups = categories.map { category ->
                CategoryGroupUi(
                    categoryId = category.id,
                    categoryEmoji = category.emoji,
                    categoryName = category.name,
                    keywords = (keywordsByCategory[category.id] ?: emptyList()).sorted()
                )
            },
            categories = categories,
            editing = editingState,
            pendingDeleteKeyword = pendingDelete
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryRulesUiState())

    /** Opens the add sheet pre-filled to the group whose "+ add" chip was tapped. */
    fun onAddClicked(categoryId: Long) {
        editing.value = EditingRuleUi(keyword = "", selectedCategoryId = categoryId, isNew = true)
    }

    fun onRuleClicked(keyword: String, categoryId: Long) {
        editing.value = EditingRuleUi(keyword = keyword, selectedCategoryId = categoryId, isNew = false)
    }

    fun onKeywordChanged(value: String) {
        editing.update { it?.copy(keyword = value) }
    }

    fun onCategorySelected(categoryId: Long) {
        editing.update { it?.copy(selectedCategoryId = categoryId) }
    }

    fun onSaveEdit() {
        val current = editing.value ?: return
        val keyword = current.keyword.trim().lowercase()
        val categoryId = current.selectedCategoryId
        if (keyword.isBlank() || categoryId == null) return
        viewModelScope.launch {
            keywordRuleRepository.upsert(keyword, categoryId)
            editing.value = null
        }
    }

    fun onEditDismissed() {
        editing.value = null
    }

    fun onDeleteRequested(keyword: String) {
        pendingDeleteKeyword.value = keyword
    }

    fun onConfirmDelete() {
        val keyword = pendingDeleteKeyword.value ?: return
        viewModelScope.launch {
            keywordRuleRepository.delete(keyword)
            pendingDeleteKeyword.value = null
        }
    }

    fun onCancelDelete() {
        pendingDeleteKeyword.value = null
    }
}
