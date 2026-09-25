package com.budgetmanager.app.domain

import com.budgetmanager.app.core.model.AlertType

/** In-memory fake for tests - records every call instead of showing a real notification. */
class FakeBudgetAlertNotifier : BudgetAlertNotifier {
    data class Call(val categoryId: Long, val categoryEmoji: String, val categoryName: String, val type: AlertType)

    val calls = mutableListOf<Call>()

    override fun showBudgetAlert(categoryId: Long, categoryEmoji: String, categoryName: String, type: AlertType) {
        calls += Call(categoryId, categoryEmoji, categoryName, type)
    }
}
