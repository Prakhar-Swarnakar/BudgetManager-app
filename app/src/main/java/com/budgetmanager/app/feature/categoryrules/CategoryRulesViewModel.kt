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
        val categoryById = categories.associateBy { it.id }
        CategoryRulesUiState(
            isLoading = false,
            // A rule whose category was somehow removed has nothing sensible to show - left out
            // rather than shown with a blank category.
            rules = rules.mapNotNull { rule ->
                categoryById[rule.categoryId]?.let { category ->
                    CategoryRuleUi(rule.keyword, rule.categoryId, category.emoji, category.name)
                }
            }.sortedBy { it.keyword },
            categories = categories,
            editing = editingState,
            pendingDeleteKeyword = pendingDelete
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryRulesUiState())

    fun onAddClicked() {
        editing.value = EditingRuleUi(keyword = "", selectedCategoryId = null, isNew = true)
    }

    fun onRuleClicked(rule: CategoryRuleUi) {
        editing.value = EditingRuleUi(keyword = rule.keyword, selectedCategoryId = rule.categoryId, isNew = false)
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
