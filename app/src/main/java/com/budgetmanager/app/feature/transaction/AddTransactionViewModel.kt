package com.budgetmanager.app.feature.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.MonthKey
import com.budgetmanager.app.data.repository.CategoryRepository
import com.budgetmanager.app.data.repository.MessageRepository
import com.budgetmanager.app.data.repository.TransactionRepository
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
                    internalState.update {
                        it.copy(
                            isLoading = false,
                            isEditMode = true,
                            transactionIdForEdit = transactionId,
                            amountInput = formatForInput(transaction.amount.paise),
                            note = transaction.note.orEmpty(),
                            date = transaction.occurredAt.atZone(ZoneId.systemDefault()).toLocalDate(),
                            selectedCategoryId = transaction.categoryId
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
                            note = message.merchant.orEmpty(),
                            date = message.receivedAt.atZone(ZoneId.systemDefault()).toLocalDate(),
                            selectedCategoryId = message.suggestedCategoryId,
                            suggestedCategoryId = message.suggestedCategoryId,
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
                            note = note
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
                        note = note
                    )
                }
                else -> {
                    transactionRepository.insert(
                        amount = amount,
                        occurredAt = occurredAt,
                        monthKey = monthKey,
                        categoryId = categoryId,
                        note = note
                    )
                }
            }
            internalState.update { it.copy(saved = true, isSaving = false) }
        }
    }

    fun onSavedHandled() {
        internalState.update { it.copy(saved = false) }
    }

    private fun formatForInput(paise: Long): String {
        val rupees = paise / 100
        val remainder = paise % 100
        return if (remainder == 0L) rupees.toString() else "%d.%02d".format(rupees, remainder)
    }
}
