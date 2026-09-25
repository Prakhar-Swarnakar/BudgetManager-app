package com.budgetmanager.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.budgetmanager.app.core.model.AlertType
import com.budgetmanager.app.data.database.entity.AlertLogEntity

@Dao
interface AlertLogDao {
    @Query("SELECT EXISTS(SELECT 1 FROM alert_log WHERE month_key = :monthKey AND category_id = :categoryId AND type = :type)")
    suspend fun hasFired(monthKey: String, categoryId: Long, type: AlertType): Boolean

    /** Every alert ever sent, for backup export. */
    @Query("SELECT * FROM alert_log")
    suspend fun getAllOnce(): List<AlertLogEntity>

    /** Ignored on conflict: the unique index means an alert already sent is never recorded twice. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun record(entry: AlertLogEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entries: List<AlertLogEntity>)

    @Query("DELETE FROM alert_log")
    suspend fun deleteAll()
}
