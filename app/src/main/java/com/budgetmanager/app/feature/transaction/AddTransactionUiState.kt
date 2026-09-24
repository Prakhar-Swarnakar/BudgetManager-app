package com.budgetmanager.app.feature.transaction

import com.budgetmanager.app.core.model.Category
import java.time.LocalDate

data class AddTransactionUiState(
    val isLoading: Boolean = true,
    val isEditMode: Boolean = false,
    val amountInput: String = "",
    val amountError: String? = null,
    val note: String = "",
    val date: LocalDate = LocalDate.now(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val categoryError: String? = null,
    val suggestedCategoryId: Long? = null,
    /** The original SMS text, shown as a banner - only set when opened from a message. */
    val smsBannerText: String? = null,
    val messageIdForAccept: Long? = null,
    val transactionIdForEdit: Long? = null,
    val saved: Boolean = false
)
