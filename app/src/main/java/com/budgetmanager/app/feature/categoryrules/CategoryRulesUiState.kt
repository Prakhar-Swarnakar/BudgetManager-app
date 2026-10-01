package com.budgetmanager.app.feature.categoryrules

import com.budgetmanager.app.core.model.Category

data class CategoryRuleUi(
    val keyword: String,
    val categoryId: Long,
    val categoryEmoji: String,
    val categoryName: String
)

/** The add/edit sheet's state. [isNew] controls whether the keyword field is editable - editing
 *  an existing rule only lets you change its category, never its keyword text, so there's never
 *  a case where changing the text leaves the old rule behind under its old key (keyword is the
 *  primary key - see KeywordRuleDao). */
data class EditingRuleUi(
    val keyword: String,
    val selectedCategoryId: Long?,
    val isNew: Boolean
)

data class CategoryRulesUiState(
    val isLoading: Boolean = true,
    val rules: List<CategoryRuleUi> = emptyList(),
    val categories: List<Category> = emptyList(),
    val editing: EditingRuleUi? = null,
    val pendingDeleteKeyword: String? = null
)
