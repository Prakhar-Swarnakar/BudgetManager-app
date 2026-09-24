package com.budgetmanager.app.data.repository

import com.budgetmanager.app.core.model.AlertType
import com.budgetmanager.app.core.model.MonthKey
import com.budgetmanager.app.data.database.dao.AlertLogDao
import com.budgetmanager.app.data.database.entity.AlertLogEntity
import javax.inject.Inject

class RoomAlertLogRepository @Inject constructor(
    private val alertLogDao: AlertLogDao
) : AlertLogRepository {
    override suspend fun hasFired(monthKey: MonthKey, categoryId: Long, type: AlertType): Boolean =
        alertLogDao.hasFired(monthKey.value, categoryId, type)

    override suspend fun record(monthKey: MonthKey, categoryId: Long, type: AlertType) {
        alertLogDao.record(AlertLogEntity(monthKey = monthKey.value, categoryId = categoryId, type = type))
    }
}
