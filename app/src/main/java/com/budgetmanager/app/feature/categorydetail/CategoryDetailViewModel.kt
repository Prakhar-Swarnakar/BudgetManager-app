package com.budgetmanager.app.feature.categorydetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmanager.app.core.model.BudgetMaths
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.MonthKey
import com.budgetmanager.app.data.repository.CategoryRepository
import com.budgetmanager.app.data.repository.MonthlyBudgetRepository
import com.budgetmanager.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CategoryDetailViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val monthlyBudgetRepository: MonthlyBudgetRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val categoryId = MutableStateFlow(0L)
    private val monthKey = MutableStateFlow(MonthKey.current())
    private val pendingDeleteTransactionId = MutableStateFlow<Long?>(null)
    private val editTransactionRequest = MutableStateFlow<Long?>(null)
    private var loadedFor: Long? = null

    /** Called once per distinct categoryId - the screen's own month navigation (below) takes
     *  over after that, same idempotency shape as AddTransactionViewModel.load(). */
    fun load(categoryId: Long, monthKey: MonthKey) {
        if (loadedFor == categoryId) return
        loadedFor = categoryId
        this.categoryId.value = categoryId
        this.monthKey.value = monthKey
    }

    val uiState = categoryId
        .flatMapLatest { id ->
            monthKey.flatMapLatest { month ->
                combine(
                    categoryRepository.observeAll(),
                    monthlyBudgetRepository.observeForMonth(month),
                    transactionRepository.observeForCategoryAndMonth(month, id),
                    pendingDeleteTransactionId,
                    editTransactionRequest
                ) { categories, budgets, transactions, pendingDelete, editRequest ->
                    val category = categories.firstOrNull { it.id == id }
                    val spent = transactions.fold(Money.Zero) { acc, t -> acc + t.amount }
                    val progress = BudgetMaths.evaluate(budgets[id] ?: Money.Zero, spent)
                    CategoryDetailUiState(
                        isLoading = false,
                        monthKey = month,
                        categoryId = id,
                        categoryEmoji = category?.emoji.orEmpty(),
                        categoryName = category?.name.orEmpty(),
                        spentText = progress.spent.formatted(),
                        budgetText = progress.budget.formatted(),
                        remainingText = progress.remaining.formatted(),
                        percentUsed = progress.percentUsed,
                        status = progress.status,
                        isNotBudgeted = progress.isNotBudgeted,
                        transactions = transactions.map { transaction ->
                            TransactionRowUi(
                                id = transaction.id,
                                noteOrPlaceholder = transaction.note?.takeIf { it.isNotBlank() } ?: "No note",
                                occurredAt = transaction.occurredAt,
                                amountText = transaction.amount.formatted(),
                                fromSms = transaction.sourceMessageId != null
                            )
                        },
                        pendingDeleteTransactionId = pendingDelete,
                        editTransactionId = editRequest
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryDetailUiState())

    fun onPreviousMonth() {
        monthKey.value = monthKey.value.previous()
    }

    fun onNextMonth() {
        monthKey.value = monthKey.value.next()
    }

    fun onTransactionClicked(transactionId: Long) {
        editTransactionRequest.value = transactionId
    }

    fun onEditNavigationHandled() {
        editTransactionRequest.value = null
    }

    /** Swipe left asks for confirmation rather than deleting straight away - see
     *  onConfirmDelete/onCancelDelete. */
    fun onDeleteSwiped(transactionId: Long) {
        pendingDeleteTransactionId.value = transactionId
    }

    fun onConfirmDelete() {
        val id = pendingDeleteTransactionId.value ?: return
        viewModelScope.launch {
            transactionRepository.delete(id)
            pendingDeleteTransactionId.value = null
        }
    }

    fun onCancelDelete() {
        pendingDeleteTransactionId.value = null
    }
}
