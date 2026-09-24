package com.budgetmanager.app.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.budgetmanager.app.data.database.AppDatabase
import com.budgetmanager.app.data.database.StarterData
import com.budgetmanager.app.data.database.dao.AlertLogDao
import com.budgetmanager.app.data.database.dao.CategoryDao
import com.budgetmanager.app.data.database.dao.KeywordRuleDao
import com.budgetmanager.app.data.database.dao.MonthlyBudgetDao
import com.budgetmanager.app.data.database.dao.SmsMessageDao
import com.budgetmanager.app.data.database.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        // A Provider, not the database directly: Room's callback fires while .build() is still
        // running, so the database can't be passed in directly without a circular dependency.
        // The Provider defers fetching the built instance until the coroutine actually runs.
        databaseProvider: Provider<AppDatabase>
    ): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                        StarterData.seed(databaseProvider.get())
                    }
                }
            })
            .build()

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideMonthlyBudgetDao(database: AppDatabase): MonthlyBudgetDao = database.monthlyBudgetDao()

    @Provides
    fun provideTransactionDao(database: AppDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideSmsMessageDao(database: AppDatabase): SmsMessageDao = database.smsMessageDao()

    @Provides
    fun provideAlertLogDao(database: AppDatabase): AlertLogDao = database.alertLogDao()

    @Provides
    fun provideKeywordRuleDao(database: AppDatabase): KeywordRuleDao = database.keywordRuleDao()
}
