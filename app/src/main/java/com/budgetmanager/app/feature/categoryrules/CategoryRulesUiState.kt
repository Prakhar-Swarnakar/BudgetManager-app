package com.budgetmanager.app.feature.categoryrules

import com.budgetmanager.app.core.model.Category

/** One category and the keywords mapped to it, in display order - every active category appears,
 *  even one with no rules yet (its "+ add" chip is the easiest way to create its first rule). */
data class CategoryGroupUi(
    val categoryId: Long,
    val categoryEmoji: String,
    val categoryName: String,
    val keywords: List<String>
)

/** The add/edit sheet's state. [isNew] controls whether the keyword field is editable - editing
 *  an existing rule only lets you change its category, never its keyword text, so there's never
 *  a case where changing the text leaves the old rule behind under its old key (keyword is the
 *  primary key - see KeywordRuleDao). [selectedCategoryId] is pre-filled to the group whose
 *  "+ add" was tapped, or to the rule's current category when editing. */
data class EditingRuleUi(
    val keyword: String,
    val selectedCategoryId: Long?,
    val isNew: Boolean
)

data class CategoryRulesUiState(
    val isLoading: Boolean = true,
    val groups: List<CategoryGroupUi> = emptyList(),
    val categories: List<Category> = emptyList(),
    val editing: EditingRuleUi? = null,
    val pendingDeleteKeyword: String? = null
)
