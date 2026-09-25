package com.budgetmanager.app.domain

import com.budgetmanager.app.core.model.MonthKey
import com.budgetmanager.app.data.repository.MonthlyBudgetRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * A new month starts as a copy of the previous month's amounts (05-budget-rules.md), so opening
 * a month with nothing set yet copies last month's budget in as a starting point. Self-gating:
 * once a month has any budget rows (whether from this copy or the user editing one directly),
 * it is no longer "empty", so calling this again for the same month is a safe no-op.
 */
class CopyBudgetFromPreviousMonth @Inject constructor(
    private val monthlyBudgetRepository: MonthlyBudgetRepository
) {
    /** Returns true if a copy actually happened, so the caller can show a "Copied from last
     *  month" note - false if the month already had amounts, or the previous month had none. */
    suspend operator fun invoke(monthKey: MonthKey): Boolean {
        val current = monthlyBudgetRepository.observeForMonth(monthKey).first()
        if (current.isNotEmpty()) return false

        val previous = monthlyBudgetRepository.observeForMonth(monthKey.previous()).first()
        if (previous.isEmpty()) return false

        previous.forEach { (categoryId, amount) ->
            monthlyBudgetRepository.setAmount(monthKey, categoryId, amount)
        }
        return true
    }
}
