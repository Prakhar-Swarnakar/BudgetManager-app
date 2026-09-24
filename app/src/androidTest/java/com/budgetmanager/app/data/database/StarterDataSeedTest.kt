package com.budgetmanager.app.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs on a phone/emulator (needs Android's real SQLite, unlike the pure-Kotlin domain tests).
 * Checks M1's "Done when": starter categories and their keywords exist after a fresh database.
 */
@RunWith(AndroidJUnit4::class)
class StarterDataSeedTest {

    private lateinit var database: AppDatabase

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun seed_createsNineStarterCategoriesAtZeroBudgetWithTheirKeywords() = runTest {
        StarterData.seed(database)

        val categories = database.categoryDao().observeAll().first()
        assertEquals(9, categories.size)
        assertTrue(categories.all { !it.archived })

        val keywordRules = database.keywordRuleDao().getAll()
        assertTrue(keywordRules.isNotEmpty())

        val foodCategory = categories.first { it.name == "Food & Dining" }
        assertTrue(keywordRules.any { it.categoryId == foodCategory.id && it.keyword == "swiggy" })

        // No monthly_budget rows are created - a category with no row shows ₹0.
        val budgetsThisMonth = database.monthlyBudgetDao().observeForMonth(MonthKeyForTest).first()
        assertTrue(budgetsThisMonth.isEmpty())
    }

    @Test
    fun seed_isIdempotent_doesNotDuplicateOnSecondCall() = runTest {
        StarterData.seed(database)
        StarterData.seed(database)

        val categories = database.categoryDao().observeAll().first()
        assertEquals(9, categories.size)
    }

    private companion object {
        const val MonthKeyForTest = "2026-09"
    }
}
