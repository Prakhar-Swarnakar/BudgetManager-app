package com.budgetmanager.app.domain

import com.budgetmanager.app.core.model.AlertType

/** The one side effect EvaluateBudgetAlerts needs - kept as a narrow interface (rather than
 *  depending on the concrete, Android-Context-requiring Notifier directly) so the alert logic
 *  itself can be unit tested with a fake, the same way every repository is. Notifier implements
 *  this. */
interface BudgetAlertNotifier {
    fun showBudgetAlert(categoryId: Long, categoryEmoji: String, categoryName: String, type: AlertType)
}
