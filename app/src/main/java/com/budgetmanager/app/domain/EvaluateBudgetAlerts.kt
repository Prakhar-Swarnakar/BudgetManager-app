package com.budgetmanager.app.domain

import com.budgetmanager.app.core.model.AlertType
import com.budgetmanager.app.core.model.BudgetMaths
import com.budgetmanager.app.core.model.BudgetStatus
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.MonthKey
import com.budgetmanager.app.data.repository.AlertLogRepository
import com.budgetmanager.app.data.repository.CategoryRepository
import com.budgetmanager.app.data.repository.MonthlyBudgetRepository
import com.budgetmanager.app.data.repository.SettingsRepository
import com.budgetmanager.app.data.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Called after a transaction is saved (manual add, or accepting a message) - never on edit or
 * delete (05-budget-rules.md). Fires at most one alert per category per month (AlertLogRepository
 * enforces that regardless of this method being called again), and if a transaction jumps
 * straight from under 80% to over budget, only the over-budget alert fires - over budget is
 * always checked first here, and the 80% check only runs when it isn't crossed.
 *
 * The alert-log record happens whether or not the switch for that type is on, so turning a
 * switch on mid-month never floods you with alerts for thresholds already crossed silently -
 * only the notification itself is gated by the switch.
 */
class EvaluateBudgetAlerts @Inject constructor(
    private val monthlyBudgetRepository: MonthlyBudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val alertLogRepository: AlertLogRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val notifier: BudgetAlertNotifier
) {
    suspend operator fun invoke(monthKey: MonthKey, categoryId: Long) {
        val budget = monthlyBudgetRepository.observeForMonth(monthKey).first()[categoryId] ?: Money.Zero
        val spent = transactionRepository.observeSpentForCategoryAndMonth(monthKey, categoryId).first()
        val progress = BudgetMaths.evaluate(budget, spent)

        when {
            progress.status == BudgetStatus.OVER_BUDGET ->
                fireIfNotAlreadyFired(monthKey, categoryId, AlertType.OVER_BUDGET)
            progress.percentUsed != null && progress.percentUsed >= 0.8 ->
                fireIfNotAlreadyFired(monthKey, categoryId, AlertType.EIGHTY_PERCENT)
        }
    }

    private suspend fun fireIfNotAlreadyFired(monthKey: MonthKey, categoryId: Long, type: AlertType) {
        if (alertLogRepository.hasFired(monthKey, categoryId, type)) return
        alertLogRepository.record(monthKey, categoryId, type)

        val switchOn = when (type) {
            AlertType.EIGHTY_PERCENT -> settingsRepository.observeEightyPercentAlertsEnabled().first()
            AlertType.OVER_BUDGET -> settingsRepository.observeOverBudgetAlertsEnabled().first()
        }
        if (!switchOn) return

        val category = categoryRepository.getById(categoryId) ?: return
        notifier.showBudgetAlert(categoryId, category.emoji, category.name, type)
    }
}
