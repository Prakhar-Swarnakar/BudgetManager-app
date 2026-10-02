package com.budgetmanager.app.feature.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.MonthKey
import com.budgetmanager.app.core.model.TaxonomyType
import com.budgetmanager.app.data.repository.CategoryRepository
import com.budgetmanager.app.data.repository.KeywordRuleRepository
import com.budgetmanager.app.data.repository.MessageRepository
import com.budgetmanager.app.data.repository.TaxonomyKeywordRuleRepository
import com.budgetmanager.app.data.repository.TransactionRepository
import com.budgetmanager.app.domain.EvaluateBudgetAlerts
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val messageRepository: MessageRepository,
    private val evaluateBudgetAlerts: EvaluateBudgetAlerts,
    private val keywordRuleRepository: KeywordRuleRepository,
    private val taxonomyKeywordRuleRepository: TaxonomyKeywordRuleRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

    private val internalState = MutableStateFlow(AddTransactionUiState())
    private var loadedFor: String? = null

    val uiState: StateFlow<AddTransactionUiState> = combine(
        internalState,
        categoryRepository.observeActive()
    ) { state, categories -> state.copy(categories = categories) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddTransactionUiState())

    /** Called once per distinct (messageId, transactionId) pair - safe to call again on
     *  recomposition, it no-ops if already loaded for these arguments. */
    fun load(messageId: Long?, transactionId: Long?) {
        val key = "$messageId:$transactionId"
        if (loadedFor == key) return
        loadedFor = key

        viewModelScope.launch {
            when {
                transactionId != null -> {
                    val transaction = transactionRepository.getById(transactionId) ?: return@launch
                    // Transaction itself has no merchant column - only a message it came from
                    // (if any) ever had one, so look there to prefill the field for editing.
                    val merchant = transaction.sourceMessageId
                        ?.let { messageRepository.getById(it)?.merchant }
                        .orEmpty()
                    internalState.update {
                        it.copy(
                            isLoading = false,
                            isEditMode = true,
                            transactionIdForEdit = transactionId,
                            amountInput = formatForInput(transaction.amount.paise),
                            merchant = merchant,
                            note = transaction.note.orEmpty(),
                            date = transaction.occurredAt.atZone(ZoneId.systemDefault()).toLocalDate(),
                            selectedCategoryId = transaction.categoryId,
                            selectedTaxonomy = transaction.taxonomy
                        )
                    }
                }
                messageId != null -> {
                    val message = messageRepository.getById(messageId) ?: return@launch
                    internalState.update {
                        it.copy(
                            isLoading = false,
                            messageIdForAccept = messageId,
                            amountInput = message.parsedAmount?.let { amount -> formatForInput(amount.paise) } ?: "",
                            merchant = message.merchant.orEmpty(),
                            // Always starts unchecked - the user opts in deliberately in every
                            // mode, rather than this defaulting on for message-linked saves.
                            addToRule = false,
                            date = message.receivedAt.atZone(ZoneId.systemDefault()).toLocalDate(),
                            selectedCategoryId = message.suggestedCategoryId,
                            suggestedCategoryId = message.suggestedCategoryId,
                            selectedTaxonomy = message.suggestedTaxonomy,
                            suggestedTaxonomy = message.suggestedTaxonomy,
                            smsBannerText = message.body
                        )
                    }
                }
                else -> internalState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onAmountChanged(value: String) {
        internalState.update { it.copy(amountInput = value, amountError = null) }
    }

    fun onMerchantChanged(value: String) {
        internalState.update {
            // Clearing the merchant also clears both checkboxes - there'd be nothing left to
            // learn, and leaving them checked would silently resurrect once a new merchant is
            // typed in.
            it.copy(
                merchant = value,
                addToRule = if (value.isBlank()) false else it.addToRule,
                addTaxonomyToRule = if (value.isBlank()) false else it.addTaxonomyToRule
            )
        }
    }

    fun onAddToRuleToggled(checked: Boolean) {
        internalState.update { it.copy(addToRule = checked) }
    }

    fun onTaxonomySelected(taxonomy: TaxonomyType) {
        internalState.update { it.copy(selectedTaxonomy = taxonomy) }
    }

    fun onAddTaxonomyToRuleToggled(checked: Boolean) {
        internalState.update { it.copy(addTaxonomyToRule = checked) }
    }

    fun onNoteChanged(value: String) {
        internalState.update { it.copy(note = value) }
    }

    fun onDateChanged(value: LocalDate) {
        internalState.update { it.copy(date = value) }
    }

    fun onCategorySelected(id: Long) {
        internalState.update { it.copy(selectedCategoryId = id, categoryError = null) }
    }

    fun onSave() {
        val current = internalState.value
        // Guards against a double-tap firing saveFromMessage twice before the screen has a
        // chance to navigate away - the second insert would violate the one-transaction-per-
        // message unique constraint and crash (a real bug hit during testing, not hypothetical).
        if (current.isSaving) return

        val amount = Money.parseRupeeInput(current.amountInput)
        val categoryId = current.selectedCategoryId

        if (amount == null) {
            internalState.update { it.copy(amountError = "Enter a valid amount") }
            return
        }
        if (categoryId == null) {
            internalState.update { it.copy(categoryError = "Choose a category") }
            return
        }

        internalState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val occurredAt = current.date.atStartOfDay(zone).toInstant()
            val monthKey = MonthKey.from(occurredAt, zone)
            val note = current.note.ifBlank { null }

            when {
                current.transactionIdForEdit != null -> {
                    val existing = transactionRepository.getById(current.transactionIdForEdit) ?: return@launch
                    transactionRepository.update(
                        existing.copy(
                            amount = amount,
                            occurredAt = occurredAt,
                            monthKey = monthKey,
                            categoryId = categoryId,
                            note = note,
                            taxonomy = current.selectedTaxonomy
                        )
                    )
                }
                current.messageIdForAccept != null -> {
                    val message = messageRepository.getById(current.messageIdForAccept) ?: return@launch
                    transactionRepository.saveFromMessage(
                        message = message,
                        amount = amount,
                        occurredAt = occurredAt,
                        monthKey = monthKey,
                        categoryId = categoryId,
                        note = note,
                        taxonomy = current.selectedTaxonomy
                    )
                    evaluateBudgetAlerts(monthKey, categoryId)
                }
                else -> {
                    transactionRepository.insert(
                        amount = amount,
                        occurredAt = occurredAt,
                        monthKey = monthKey,
                        categoryId = categoryId,
                        note = note,
                        taxonomy = current.selectedTaxonomy
                    )
                    evaluateBudgetAlerts(monthKey, categoryId)
                }
            }
            if (current.addToRule) {
                learnFromChoice(current.merchant, categoryId)
            }
            if (current.addTaxonomyToRule) {
                learnTaxonomyFromChoice(current.merchant, current.selectedTaxonomy)
            }
            internalState.update { it.copy(saved = true, isSaving = false) }
        }
    }

    fun onSavedHandled() {
        internalState.update { it.copy(saved = false) }
    }

    /** Remembers merchant -> category so next time the same merchant shows up it's suggested
     *  automatically (06-backlog.md, "Learning from choices") - called from any save (manual,
     *  message-linked, or edit) when the user has "Add to rule" checked, not just on accepting a
     *  message. Skipped for very short merchant text - a one- or two-character match is more
     *  likely to misfire against an unrelated future message than to help. Always takes the
     *  category the user actually chose here, even overriding an existing rule for the same
     *  merchant - that's the point of learning from a correction. */
    private suspend fun learnFromChoice(merchant: String?, categoryId: Long) {
        val keyword = merchant?.trim()?.lowercase() ?: return
        if (keyword.length < 3) return
        keywordRuleRepository.upsert(keyword, categoryId)
    }

    /** Same idea as [learnFromChoice], for the separate taxonomy rule set. */
    private suspend fun learnTaxonomyFromChoice(merchant: String?, taxonomy: TaxonomyType?) {
        val keyword = merchant?.trim()?.lowercase() ?: return
        if (keyword.length < 3 || taxonomy == null) return
        taxonomyKeywordRuleRepository.upsert(keyword, taxonomy)
    }

    private fun formatForInput(paise: Long): String {
        val rupees = paise / 100
        val remainder = paise % 100
        return if (remainder == 0L) rupees.toString() else "%d.%02d".format(rupees, remainder)
    }
}
