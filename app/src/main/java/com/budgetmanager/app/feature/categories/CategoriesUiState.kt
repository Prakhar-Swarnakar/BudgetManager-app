package com.budgetmanager.app.feature.categories

import com.budgetmanager.app.core.model.Category

data class CategoriesUiState(
    val isLoading: Boolean = true,
    val activeCategories: List<Category> = emptyList(),
    val archivedCategories: List<Category> = emptyList(),
    val archivedExpanded: Boolean = false,
    val sheet: CategorySheetUiState? = null
)

/** [editingCategoryId] is null when the sheet is creating a new category, and set when it is
 *  renaming or changing the icon of an existing one. */
data class CategorySheetUiState(
    val editingCategoryId: Long? = null,
    val name: String = "",
    val emoji: String = "",
    val nameError: String? = null,
    val emojiError: String? = null
)
