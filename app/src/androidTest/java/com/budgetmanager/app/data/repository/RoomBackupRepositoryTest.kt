package com.budgetmanager.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.budgetmanager.app.core.model.AlertType
import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.data.backup.AlertLogBackup
import com.budgetmanager.app.data.backup.BackupBundle
import com.budgetmanager.app.data.backup.CategoryBackup
import com.budgetmanager.app.data.backup.KeywordRuleBackup
import com.budgetmanager.app.data.backup.MonthlyBudgetBackup
import com.budgetmanager.app.data.backup.SmsMessageBackup
import com.budgetmanager.app.data.backup.TransactionBackup
import com.budgetmanager.app.data.database.AppDatabase
import com.budgetmanager.app.data.database.entity.CategoryEntity
import com.budgetmanager.app.data.database.entity.MonthlyBudgetEntity
import com.budgetmanager.app.data.database.entity.SmsMessageEntity
import com.budgetmanager.app.data.database.entity.TransactionEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomBackupRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: BackupRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        repository = RoomBackupRepository(
            database, database.categoryDao(), database.monthlyBudgetDao(), database.transactionDao(),
            database.smsMessageDao(), database.alertLogDao(), database.keywordRuleDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun buildBackup_reflectsEveryTableIncludingForeignKeys() = runTest {
        val categoryId = database.categoryDao().insert(CategoryEntity(name = "Food", emoji = "🍔", sortOrder = 0))
        val messageId = database.smsMessageDao().insert(
            SmsMessageEntity(
                sender = "BANK", body = "Rs 100 debited", receivedAt = 1L, smsProviderId = null,
                dedupeKey = "d1", parsedAmountPaise = 10000, merchant = "M", paymentMethod = "UPI",
                suggestedCategoryId = categoryId, status = MessageStatus.ACCEPTED, isNew = false
            )
        )
        database.transactionDao().insert(
            TransactionEntity(
                amountPaise = 10000, occurredAt = 1L, monthKey = "2026-09", categoryId = categoryId,
                note = null, sourceMessageId = messageId
            )
        )
        database.monthlyBudgetDao().upsert(MonthlyBudgetEntity(monthKey = "2026-09", categoryId = categoryId, amountPaise = 500000))

        val bundle = repository.buildBackup()

        assertEquals(1, bundle.categories.size)
        assertEquals(1, bundle.transactions.size)
        assertEquals(1, bundle.smsMessages.size)
        assertEquals(1, bundle.monthlyBudgets.size)
        assertEquals(categoryId, bundle.transactions.single().categoryId)
        assertEquals(messageId, bundle.transactions.single().sourceMessageId)
        assertEquals(categoryId, bundle.smsMessages.single().suggestedCategoryId)
    }

    @Test
    fun restore_replacesExistingDataAndPreservesForeignKeys() = runTest {
        // Something already in the database that must be gone after restore.
        database.categoryDao().insert(CategoryEntity(name = "Old", emoji = "🗑️", sortOrder = 0))

        val bundle = BackupBundle(
            formatVersion = BackupBundle.CURRENT_FORMAT_VERSION,
            exportedAtMillis = 1L,
            categories = listOf(CategoryBackup(5, "Groceries", "🛒", 0, false)),
            monthlyBudgets = listOf(MonthlyBudgetBackup(9, "2026-09", 5, 300000)),
            transactions = listOf(TransactionBackup(7, 15000, 2L, "2026-09", 5, "note", 3)),
            smsMessages = listOf(
                SmsMessageBackup(
                    3, "BANK", "body", 2L, null, "dedupe-x", 15000, "Merchant",
                    "UPI", 5, MessageStatus.ACCEPTED, false
                )
            ),
            alertLogs = listOf(AlertLogBackup(11, "2026-09", 5, AlertType.EIGHTY_PERCENT)),
            keywordRules = listOf(KeywordRuleBackup("bigbasket", 5))
        )

        repository.restore(bundle)

        val categories = database.categoryDao().getAllOnce()
        assertEquals(1, categories.size)
        assertEquals(5L, categories.single().id)
        assertEquals("Groceries", categories.single().name)

        val transactions = database.transactionDao().getAllOnce()
        assertEquals(7L, transactions.single().id)
        assertEquals(5L, transactions.single().categoryId)
        assertEquals(3L, transactions.single().sourceMessageId)

        val messages = database.smsMessageDao().getAllOnce()
        assertEquals(3L, messages.single().id)
        assertEquals(5L, messages.single().suggestedCategoryId)

        assertEquals(1, database.monthlyBudgetDao().getAllOnce().size)
        assertEquals(1, database.alertLogDao().getAllOnce().size)
        assertEquals(1, database.keywordRuleDao().getAll().size)
    }

    @Test
    fun exportThenImport_reproducesIdenticalData() = runTest {
        val categoryId = database.categoryDao().insert(CategoryEntity(name = "Food", emoji = "🍔", sortOrder = 0))
        database.monthlyBudgetDao().upsert(MonthlyBudgetEntity(monthKey = "2026-09", categoryId = categoryId, amountPaise = 500000))
        val messageId = database.smsMessageDao().insert(
            SmsMessageEntity(
                sender = "BANK", body = "Rs 100 debited", receivedAt = 1L, smsProviderId = null,
                dedupeKey = "d1", parsedAmountPaise = 10000, merchant = "M", paymentMethod = "UPI",
                suggestedCategoryId = categoryId, status = MessageStatus.ACCEPTED, isNew = false
            )
        )
        database.transactionDao().insert(
            TransactionEntity(
                amountPaise = 10000, occurredAt = 1L, monthKey = "2026-09", categoryId = categoryId,
                note = null, sourceMessageId = messageId
            )
        )

        val exported = repository.buildBackup()

        // Simulate "clear storage, then import" by wiping the tables and restoring from the export.
        repository.restore(exported)
        val reimported = repository.buildBackup()

        assertEquals(exported.categories, reimported.categories)
        assertEquals(exported.transactions, reimported.transactions)
        assertEquals(exported.smsMessages, reimported.smsMessages)
        assertEquals(exported.monthlyBudgets, reimported.monthlyBudgets)
        assertTrue(reimported.exportedAtMillis > 0)
    }
}
