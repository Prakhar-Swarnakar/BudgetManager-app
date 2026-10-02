package com.budgetmanager.app.feature.taxonomyrules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmanager.app.core.model.TaxonomyType
import com.budgetmanager.app.data.repository.TaxonomyKeywordRuleRepository
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
class TaxonomyRulesViewModel @Inject constructor(
    private val taxonomyKeywordRuleRepository: TaxonomyKeywordRuleRepository
) : ViewModel() {

    private val editing = MutableStateFlow<EditingTaxonomyRuleUi?>(null)
    private val pendingDeleteKeyword = MutableStateFlow<String?>(null)

    val uiState: StateFlow<TaxonomyRulesUiState> = combine(
        taxonomyKeywordRuleRepository.observeAll(),
        editing,
        pendingDeleteKeyword
    ) { rules, editingState, pendingDelete ->
        val keywordsByTaxonomy = rules.groupBy({ it.taxonomy }, valueTransform = { it.keyword })
        TaxonomyRulesUiState(
            isLoading = false,
            // Every taxonomy value gets a group, even an empty one - the set is fixed, so there's
            // no "+ create a new one" the way Category has.
            groups = TaxonomyType.entries.map { taxonomy ->
                TaxonomyGroupUi(
                    taxonomy = taxonomy,
                    keywords = (keywordsByTaxonomy[taxonomy] ?: emptyList()).sorted()
                )
            },
            editing = editingState,
            pendingDeleteKeyword = pendingDelete
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaxonomyRulesUiState())

    /** Opens the add sheet pre-filled to the group whose "+ add" chip was tapped. */
    fun onAddClicked(taxonomy: TaxonomyType) {
        editing.value = EditingTaxonomyRuleUi(keyword = "", selectedTaxonomy = taxonomy, isNew = true)
    }

    fun onRuleClicked(keyword: String, taxonomy: TaxonomyType) {
        editing.value = EditingTaxonomyRuleUi(keyword = keyword, selectedTaxonomy = taxonomy, isNew = false)
    }

    fun onKeywordChanged(value: String) {
        editing.update { it?.copy(keyword = value) }
    }

    fun onTaxonomySelected(taxonomy: TaxonomyType) {
        editing.update { it?.copy(selectedTaxonomy = taxonomy) }
    }

    fun onSaveEdit() {
        val current = editing.value ?: return
        val keyword = current.keyword.trim().lowercase()
        val taxonomy = current.selectedTaxonomy
        if (keyword.isBlank() || taxonomy == null) return
        viewModelScope.launch {
            taxonomyKeywordRuleRepository.upsert(keyword, taxonomy)
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
            taxonomyKeywordRuleRepository.delete(keyword)
            pendingDeleteKeyword.value = null
        }
    }

    fun onCancelDelete() {
        pendingDeleteKeyword.value = null
    }
}
