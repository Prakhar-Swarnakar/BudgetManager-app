package com.budgetmanager.app.feature.categorydetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmanager.app.core.model.BudgetMaths
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.MonthKey
import com.budgetmanager.app.data.repository.CategoryRepository
import com.budgetmanager.app.data.repository.MessageRepository
import com.budgetmanager.app.data.repository.MonthlyBudgetRepository
import com.budgetmanager.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    private val transactionRepository: TransactionRepository,
    private val messageRepository: MessageRepository
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

    val uiState: StateFlow<CategoryDetailUiState> = categoryId
        .flatMapLatest { id ->
            monthKey.flatMapLatest { month ->
                combine(
                    categoryRepository.observeAll(),
                    monthlyBudgetRepository.observeForMonth(month),
                    transactionRepository.observeForCategoryAndMonth(month, id),
                    // The same query Home's cards are built from, rather than re-summing the
                    // list here: money split with friends must not count toward the budget, and
                    // summing transaction.amount locally counted it, so this page's header
                    // disagreed with Home's card for the same category whenever anything on it
                    // had been split.
                    transactionRepository.observeSpentForCategoryAndMonth(month, id),
                    pendingDeleteTransactionId
                ) { categories, budgets, transactions, spent, pendingDelete ->
                    val category = categories.firstOrNull { it.id == id }
                    val progress = BudgetMaths.evaluate(budgets[id] ?: Money.Zero, spent)
                    // One query for every SMS-linked transaction's message, rather than a
                    // suspend getById per row inside the mapping below.
                    val linkedMessages =
                        messageRepository.getByIds(transactions.mapNotNull { it.sourceMessageId })
                    CategoryDetailUiState(
                        isLoading = false,
                        monthKey = month,
                        canGoNext = month < MonthKey.current(),
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
                            // transaction.title is what Add Transaction's "Title" field actually
                            // saves - null for anything saved before that field existed, or left
                            // blank, in which case fall back to the linked message's own merchant
                            // (never touched by a title edit), then the note, then a placeholder.
                            // occurredAt is always midnight (Add Transaction's date field has no
                            // time component) - the linked message's own receivedAt was never
                            // touched, so prefer it for the real time regardless of title.
                            val linkedMessage = transaction.sourceMessageId?.let { linkedMessages[it] }
                            val title = transaction.title?.takeIf { it.isNotBlank() }
                                ?: linkedMessage?.merchant?.takeIf { it.isNotBlank() }
                                ?: transaction.note?.takeIf { it.isNotBlank() }
                                ?: "No note"
                            // "Your share" plus the original amount struck through, once this
                            // transaction is actually split with friends (06-backlog.md, "Split
                            // with friends") - unchanged for every non-split transaction.
                            val yourShare = transaction.amount - transaction.owedTotal
                            val isSplit = transaction.owedTotal.paise > 0
                            TransactionRowUi(
                                id = transaction.id,
                                noteOrPlaceholder = title,
                                occurredAt = linkedMessage?.receivedAt ?: transaction.occurredAt,
                                amountText = if (isSplit) yourShare.formatted() else transaction.amount.formatted(),
                                splitTotalText = if (isSplit) transaction.amount.formatted() else null,
                                fromSms = transaction.sourceMessageId != null
                            )
                        },
                        pendingDeleteTransactionId = pendingDelete
                    )
                }
            }
        }
        .combine(editTransactionRequest) { state, editRequest ->
            state.copy(editTransactionId = editRequest)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryDetailUiState())

    fun onPreviousMonth() {
        monthKey.value = monthKey.value.previous()
    }

    /** Same restriction as Home and Messages: a category's actual spending is tied to dates that
     *  have already happened, so there is never a future month to look at. Monthly budget is the
     *  one page that does allow it, for setting next month's budget ahead of time. */
    fun onNextMonth() {
        if (monthKey.value < MonthKey.current()) {
            monthKey.value = monthKey.value.next()
        }
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
