package com.budgetmanager.app.data.repository

import com.budgetmanager.app.core.model.AlertType
import com.budgetmanager.app.core.model.MonthKey

class FakeAlertLogRepository : AlertLogRepository {
    private val fired = mutableSetOf<Triple<MonthKey, Long, AlertType>>()

    override suspend fun hasFired(monthKey: MonthKey, categoryId: Long, type: AlertType): Boolean =
        Triple(monthKey, categoryId, type) in fired

    override suspend fun record(monthKey: MonthKey, categoryId: Long, type: AlertType) {
        fired += Triple(monthKey, categoryId, type)
    }
}
