package com.budgetmanager.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.budgetmanager.app.data.database.entity.MonthlyBudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyBudgetDao {
    @Query("SELECT * FROM monthly_budget WHERE month_key = :monthKey")
    fun observeForMonth(monthKey: String): Flow<List<MonthlyBudgetEntity>>

    @Query("SELECT * FROM monthly_budget WHERE month_key = :monthKey AND category_id = :categoryId LIMIT 1")
    suspend fun get(monthKey: String, categoryId: Long): MonthlyBudgetEntity?

    /** Replaces any existing amount for this (month, category) pair - the unique index makes
     *  this behave as an upsert rather than always inserting a new row. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: MonthlyBudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(budgets: List<MonthlyBudgetEntity>)
}
