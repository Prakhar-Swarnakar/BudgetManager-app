package com.budgetmanager.app.data.di

import com.budgetmanager.app.data.repository.AlertLogRepository
import com.budgetmanager.app.data.repository.CategoryRepository
import com.budgetmanager.app.data.repository.DataStoreSettingsRepository
import com.budgetmanager.app.data.repository.KeywordRuleRepository
import com.budgetmanager.app.data.repository.MessageRepository
import com.budgetmanager.app.data.repository.MonthlyBudgetRepository
import com.budgetmanager.app.data.repository.RoomAlertLogRepository
import com.budgetmanager.app.data.repository.RoomCategoryRepository
import com.budgetmanager.app.data.repository.RoomKeywordRuleRepository
import com.budgetmanager.app.data.repository.RoomMessageRepository
import com.budgetmanager.app.data.repository.RoomMonthlyBudgetRepository
import com.budgetmanager.app.data.repository.RoomTransactionRepository
import com.budgetmanager.app.data.repository.SettingsRepository
import com.budgetmanager.app.data.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: RoomCategoryRepository): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindMonthlyBudgetRepository(impl: RoomMonthlyBudgetRepository): MonthlyBudgetRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: RoomTransactionRepository): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindMessageRepository(impl: RoomMessageRepository): MessageRepository

    @Binds
    @Singleton
    abstract fun bindAlertLogRepository(impl: RoomAlertLogRepository): AlertLogRepository

    @Binds
    @Singleton
    abstract fun bindKeywordRuleRepository(impl: RoomKeywordRuleRepository): KeywordRuleRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository
}
