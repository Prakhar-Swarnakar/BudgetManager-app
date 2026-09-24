package com.budgetmanager.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.budgetmanager.app.data.database.dao.AlertLogDao
import com.budgetmanager.app.data.database.dao.CategoryDao
import com.budgetmanager.app.data.database.dao.KeywordRuleDao
import com.budgetmanager.app.data.database.dao.MonthlyBudgetDao
import com.budgetmanager.app.data.database.dao.SmsMessageDao
import com.budgetmanager.app.data.database.dao.TransactionDao
import com.budgetmanager.app.data.database.entity.AlertLogEntity
import com.budgetmanager.app.data.database.entity.CategoryEntity
import com.budgetmanager.app.data.database.entity.KeywordRuleEntity
import com.budgetmanager.app.data.database.entity.MonthlyBudgetEntity
import com.budgetmanager.app.data.database.entity.SmsMessageEntity
import com.budgetmanager.app.data.database.entity.TransactionEntity

/** Version 1. Every future schema change bumps this and ships a tested migration - never
 *  fallbackToDestructiveMigration, that deletes the user's history. See 13-development-best-practices.md. */
@Database(
    entities = [
        CategoryEntity::class,
        MonthlyBudgetEntity::class,
        TransactionEntity::class,
        SmsMessageEntity::class,
        AlertLogEntity::class,
        KeywordRuleEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun monthlyBudgetDao(): MonthlyBudgetDao
    abstract fun transactionDao(): TransactionDao
    abstract fun smsMessageDao(): SmsMessageDao
    abstract fun alertLogDao(): AlertLogDao
    abstract fun keywordRuleDao(): KeywordRuleDao

    companion object {
        const val NAME = "budget_manager.db"
    }
}
