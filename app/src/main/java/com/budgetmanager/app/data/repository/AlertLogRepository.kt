package com.budgetmanager.app.data.repository

import com.budgetmanager.app.core.model.AlertType
import com.budgetmanager.app.core.model.MonthKey

interface AlertLogRepository {
    suspend fun hasFired(monthKey: MonthKey, categoryId: Long, type: AlertType): Boolean
    suspend fun record(monthKey: MonthKey, categoryId: Long, type: AlertType)
}
