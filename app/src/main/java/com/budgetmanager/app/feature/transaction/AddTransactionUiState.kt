package com.budgetmanager.app.feature.transaction

import com.budgetmanager.app.core.model.Category
import com.budgetmanager.app.core.model.TaxonomyType
import java.time.LocalDate

data class AddTransactionUiState(
    val isLoading: Boolean = true,
    val isEditMode: Boolean = false,
    val amountInput: String = "",
    val amountError: String? = null,
    val merchant: String = "",
    /** Whether saving should also upsert (merchant, category) as a keyword rule. Disabled in the
     *  UI whenever [merchant] is blank - nothing to learn from. */
    val addToRule: Boolean = false,
    val note: String = "",
    val date: LocalDate = LocalDate.now(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val categoryError: String? = null,
    val suggestedCategoryId: Long? = null,
    val selectedTaxonomy: TaxonomyType? = null,
    val suggestedTaxonomy: TaxonomyType? = null,
    /** Mirrors [addToRule] for the separate taxonomy rule set - independent, since you might
     *  want to remember a merchant's category but not its taxonomy, or vice versa. */
    val addTaxonomyToRule: Boolean = false,
    /** The original SMS text, shown as a banner - only set when opened from a message. */
    val smsBannerText: String? = null,
    val messageIdForAccept: Long? = null,
    val transactionIdForEdit: Long? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false
)
