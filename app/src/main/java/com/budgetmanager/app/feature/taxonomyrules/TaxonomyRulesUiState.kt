package com.budgetmanager.app.feature.taxonomyrules

import com.budgetmanager.app.core.model.TaxonomyType

/** One taxonomy value and the keywords mapped to it, in display order - every value in
 *  [TaxonomyType.entries] appears, even one with no rules yet, since the set is fixed (unlike
 *  Category there's no "+ create a new one" - its "+ add" chip is the easiest way to create its
 *  first rule). */
data class TaxonomyGroupUi(
    val taxonomy: TaxonomyType,
    val keywords: List<String>
)

/** The add/edit sheet's state. [isNew] controls whether the keyword field is editable - same
 *  reason as Category rules' EditingRuleUi (keyword is the primary key). */
data class EditingTaxonomyRuleUi(
    val keyword: String,
    val selectedTaxonomy: TaxonomyType?,
    val isNew: Boolean
)

data class TaxonomyRulesUiState(
    val isLoading: Boolean = true,
    val groups: List<TaxonomyGroupUi> = emptyList(),
    val editing: EditingTaxonomyRuleUi? = null,
    val pendingDeleteKeyword: String? = null
)
