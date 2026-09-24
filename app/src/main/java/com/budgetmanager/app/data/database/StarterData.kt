package com.budgetmanager.app.data.database

import com.budgetmanager.app.data.database.entity.CategoryEntity
import com.budgetmanager.app.data.database.entity.KeywordRuleEntity

/**
 * The starter categories and keyword suggestions created on first launch, so setup takes
 * a minute instead of an hour and suggestions work on day one. See 02-features.md (F1, F7).
 * Budgets start at ₹0 - the Monthly budget page is where amounts get set.
 */
object StarterData {

    data class Starter(val name: String, val emoji: String, val keywords: List<String>)

    val categories = listOf(
        Starter("Rent", "🏠", listOf("rent")),
        Starter("Groceries", "🛒", listOf("bigbasket", "blinkit", "zepto", "grofers", "dmart", "grocery")),
        Starter(
            "Food & Dining", "🍔",
            listOf("swiggy", "zomato", "restaurant", "cafe", "dominos", "mcdonald", "kfc", "starbucks")
        ),
        Starter("Transport", "🚗", listOf("uber", "ola", "rapido", "irctc", "metro", "petrol", "fuel")),
        Starter(
            "Bills & Utilities", "💡",
            listOf("electricity", "recharge", "broadband", "airtel", "jio", "vodafone", "gas")
        ),
        Starter(
            "Entertainment", "🎬",
            listOf("netflix", "hotstar", "spotify", "bookmyshow", "pvr", "inox", "prime video")
        ),
        Starter("Shopping", "🛍️", listOf("amazon", "flipkart", "myntra", "ajio")),
        Starter("Health", "🏥", listOf("pharmacy", "apollo", "hospital", "clinic", "medplus")),
        Starter("Other", "📦", emptyList())
    )

    /** Idempotent: does nothing if categories already exist, so it is safe to call on every launch. */
    suspend fun seed(database: AppDatabase) {
        val categoryDao = database.categoryDao()
        if (categoryDao.count() > 0) return

        val keywordRuleDao = database.keywordRuleDao()
        categories.forEachIndexed { index, starter ->
            val categoryId = categoryDao.insert(
                CategoryEntity(name = starter.name, emoji = starter.emoji, sortOrder = index)
            )
            if (starter.keywords.isNotEmpty()) {
                keywordRuleDao.insertAll(
                    starter.keywords.map { keyword -> KeywordRuleEntity(keyword = keyword, categoryId = categoryId) }
                )
            }
        }
    }
}
