package com.budgetmanager.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmanager.app.core.model.BudgetMaths
import com.budgetmanager.app.core.model.MessageStatus
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val monthlyBudgetRepository: MonthlyBudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val messageRepository: MessageRepository
) : ViewModel() {

    private val monthKey = MutableStateFlow(MonthKey.current())

    val uiState = monthKey
        .flatMapLatest { month ->
            combine(
                categoryRepository.observeActive(),
                monthlyBudgetRepository.observeForMonth(month),
                transactionRepository.observeSpentByCategoryForMonth(month),
                messageRepository.observeCountByStatus(MessageStatus.NOT_ASSIGNED)
            ) { categories, budgets, spentByCategory, reviewCount ->
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
                        status = progress.status,
                        isNotBudgeted = progress.isNotBudgeted
                    )
                }
                val totalBudget = budgets.values.fold(Money.Zero) { acc, m -> acc + m }
                val totalSpent = spentByCategory.values.fold(Money.Zero) { acc, m -> acc + m }
                val totalProgress = BudgetMaths.evaluate(totalBudget, totalSpent)
                HomeUiState(
                    isLoading = false,
                    monthKey = month,
                    canGoNext = month < MonthKey.current(),
                    totalSpentText = totalProgress.spent.formatted(),
                    totalBudgetText = totalProgress.budget.formatted(),
                    totalRemainingText = totalProgress.remaining.formatted(),
                    totalPercentUsed = totalProgress.percentUsed,
                    totalStatus = totalProgress.status,
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
}
