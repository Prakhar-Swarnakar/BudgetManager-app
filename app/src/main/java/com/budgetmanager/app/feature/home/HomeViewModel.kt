package com.budgetmanager.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmanager.app.core.model.BudgetMaths
import com.budgetmanager.app.core.model.BudgetProgress
import com.budgetmanager.app.core.model.MessageDirection
import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.MonthKey
import com.budgetmanager.app.data.repository.CashRepository
import com.budgetmanager.app.data.repository.CategoryRepository
import com.budgetmanager.app.data.repository.MessageRepository
import com.budgetmanager.app.data.repository.MonthlyBudgetRepository
import com.budgetmanager.app.data.repository.TransactionRepository
import com.budgetmanager.app.data.repository.TransactionSplitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val monthlyBudgetRepository: MonthlyBudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val messageRepository: MessageRepository,
    private val transactionSplitRepository: TransactionSplitRepository,
    private val cashRepository: CashRepository
) : ViewModel() {

    /** Overridable by tests so pace colours don't depend on the day the suite runs. */
    internal var today: () -> LocalDate = { LocalDate.now() }

    private data class Side(val income: Money, val owed: Money, val cash: Money, val avoidable: Money)

    private val monthKey = MutableStateFlow(MonthKey.current())

    val uiState: StateFlow<HomeUiState> = monthKey
        .flatMapLatest { month ->
            combine(
                categoryRepository.observeActive(),
                monthlyBudgetRepository.observeForMonth(month),
                transactionRepository.observeSpentByCategoryForMonth(month),
                messageRepository.observeCountByStatus(MessageStatus.NOT_ASSIGNED),
                // Every accepted credit counts as income for now - there's no typed Refund/
                // Settlement/Income distinction yet (06-backlog.md, "Credit handling"), so a
                // friend settling up or a refund would also count; revisit once that's built.
                combine(
                    messageRepository.observeAmountTotalForMonth(
                        month, MessageStatus.ACCEPTED, MessageDirection.CREDIT
                    ),
                    // Still-pending friend shares on transactions dated in the viewed month - the
                    // same unsettled figure Owed to you's own Pending total uses, just narrowed
                    // to this month. A transaction's date, not when the friend pays back.
                    transactionSplitRepository.observeOwedToYou(),
                    // Same balance the Wallet page shows: top-ups minus Cash spends, all time.
                    combine(cashRepository.observeTopUps(), cashRepository.observeSpends()) { topUps, spends ->
                        Money(topUps.sumOf { it.amount.paise } - spends.sumOf { it.amount.paise })
                    },
                    transactionRepository.observeAvoidableForMonth(month)
                ) { income, owedEntries, cash, avoidable ->
                    val (start, end) = month.toMillisRange(ZoneId.systemDefault())
                    val owed = owedEntries
                        .filter { it.occurredAt.toEpochMilli() in start until end }
                        .fold(Money.Zero) { acc, entry -> acc + entry.totalOwed }
                    Side(income, owed, cash, avoidable)
                }
            ) { categories, budgets, spentByCategory, reviewCount, side ->
                val totalIncome = side.income
                val totalOwed = side.owed
                val todayDate = today()
                val pace = paceFraction(month, todayDate)
                val cards = categories.map { category ->
                    val progress = BudgetMaths.evaluate(
                        budget = budgets[category.id] ?: Money.Zero,
                        spent = spentByCategory[category.id] ?: Money.Zero
                    )
                    HomeCategoryCardUi(
                        categoryId = category.id,
                        emoji = category.emoji,
                        name = category.name,
                        spentText = progress.spent.formatted(),
                        remainingText = progress.remaining.formatted(),
                        percentUsed = progress.percentUsed,
                        status = paceStatus(progress, pace),
                        leftLabel = leftLabel(progress),
                        isNotBudgeted = progress.isNotBudgeted
                    )
                }
                val totalBudget = budgets.values.fold(Money.Zero) { acc, m -> acc + m }
                val totalSpent = spentByCategory.values.fold(Money.Zero) { acc, m -> acc + m }
                val totalProgress = BudgetMaths.evaluate(totalBudget, totalSpent)
                val totalSaved = totalIncome - totalSpent
                HomeUiState(
                    isLoading = false,
                    monthKey = month,
                    canGoNext = month < MonthKey.current(),
                    totalSpentText = totalProgress.spent.formatted(),
                    totalBudgetText = totalProgress.budget.formatted(),
                    totalRemainingText = totalProgress.remaining.formatted(),
                    totalPercentUsed = totalProgress.percentUsed,
                    totalStatus = paceStatus(totalProgress, pace),
                    totalLeftLabel = leftLabel(totalProgress),
                    paceFraction = pace,
                    paceText = paceText(totalProgress, pace, todayDate),
                    cashOnHandText = side.cash.formatted(),
                    avoidableText = side.avoidable.formatted(),
                    avoidableShareText = if (totalSpent.paise > 0 && side.avoidable.paise > 0) {
                        "${(side.avoidable.paise * 100 / totalSpent.paise)}% of spent"
                    } else null,
                    totalIncomeText = totalIncome.formatted(),
                    totalSavedText = totalSaved.formatted(),
                    totalOwedText = totalOwed.formatted(),
                    reviewCount = reviewCount,
                    cards = cards
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onPreviousMonth() {
        monthKey.value = monthKey.value.previous()
    }

    fun onNextMonth() {
        if (monthKey.value < MonthKey.current()) {
            monthKey.value = monthKey.value.next()
        }
    }

    private fun leftLabel(progress: BudgetProgress): String = when {
        progress.isNotBudgeted -> "Not budgeted"
        progress.remaining.paise < 0 -> "${(-progress.remaining).formatted()} over"
        else -> "${progress.remaining.formatted()} left"
    }

    private fun paceText(progress: BudgetProgress, pace: Double?, todayDate: LocalDate): String? {
        if (pace == null || progress.budget.paise <= 0) return null
        val diff = paceDifference(progress, pace)
        val day = todayDate.format(DateTimeFormatter.ofPattern("d MMM"))
        return when {
            diff.paise > 0 -> "${diff.formatted()} over pace for $day"
            diff.paise < 0 -> "${(-diff).formatted()} under pace for $day"
            else -> "On pace for $day"
        }
    }
}
