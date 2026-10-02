package com.budgetmanager.app.domain

import com.budgetmanager.app.core.model.TaxonomyType
import com.budgetmanager.app.data.repository.CategoryRepository
import com.budgetmanager.app.data.repository.KeywordRuleRepository
import com.budgetmanager.app.data.repository.SettingsRepository
import com.budgetmanager.app.data.repository.TaxonomyKeywordRuleRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Merchant -> category keywords from real on-device SMS analysis (2026-10-02), covering
 *  merchants not already in StarterData's starter keyword lists. "google india" and
 *  "policybazaar" were deliberately left out - flagged as open category judgment calls, not
 *  settled, so the user can add them manually via Category rules if they want. */
private val categoryTaxonomy: Map<String, List<String>> = mapOf(
    "Food & Dining" to listOf(
        "zomato", "eternal", "eatclub", "compass india", "corporate cafe",
        "yum restaurant", "burgerking", "munchmart"
    ),
    "Groceries" to listOf("swiggyinstamart", "luluvaluemart", "trent hyper", "reliance retail"),
    "Transport" to listOf("olacabs", "yulu", "zoomcar", "indigo", "redbus"),
    "Entertainment" to listOf("jiohotstar", "pvrinox", "sunburn"),
    "Shopping" to listOf("zara", "tanishq"),
    "Bills & Utilities" to listOf("cred"),
    "Health" to listOf("1mg", "mosaic wellness", "lifecare", "sakraworldhospital")
)

/** Starter keywords for the payment-method taxonomy - modest and conservative, since a merchant
 *  name doesn't reliably imply payment method the way it implies category (real SMS data showed
 *  payment method is mostly determined by which bank sent the SMS, not by merchant). These are
 *  generic payment-method words, not merchant names. */
private val taxonomyTaxonomy: Map<TaxonomyType, List<String>> = mapOf(
    TaxonomyType.UPI to listOf("upi", "gpay", "google pay", "phonepe", "paytm"),
    TaxonomyType.CREDIT_CARD to listOf("credit card"),
    TaxonomyType.DEBIT_CARD to listOf("debit card"),
    TaxonomyType.BANK_TRANSFER to listOf("neft", "imps", "rtgs"),
    TaxonomyType.WALLET to listOf("wallet"),
    TaxonomyType.CASH to listOf("cash")
)

/**
 * Backfills both keyword taxonomies into an existing install, once - StarterData.seed() only
 * ever runs on a brand-new database (see data/database/StarterData.kt), so it can never reach a
 * device that already has categories, like this one. Gated by SettingsRepository's one-time
 * flag; safe to call on every app open (self-guards after the first real run).
 *
 * Never overwrites a keyword the user has already set differently via Category rules or
 * Taxonomy rules - each keyword is only inserted if it doesn't already exist in its respective
 * table. Never creates a category. Skips gracefully if a category name doesn't match (e.g.
 * renamed).
 */
class SeedKeywordTaxonomy @Inject constructor(
    private val keywordRuleRepository: KeywordRuleRepository,
    private val taxonomyKeywordRuleRepository: TaxonomyKeywordRuleRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke() {
        if (settingsRepository.isTaxonomyKeywordsSeeded()) return

        seedCategoryTaxonomy()
        seedTaxonomyTaxonomy()

        settingsRepository.setTaxonomyKeywordsSeeded(true)
    }

    private suspend fun seedCategoryTaxonomy() {
        val existingKeys = keywordRuleRepository.getAll().map { it.keyword.lowercase() }.toSet()
        val categoryIdByName = categoryRepository.observeActive().first().associate { it.name to it.id }

        categoryTaxonomy.forEach { (categoryName, keywords) ->
            val categoryId = categoryIdByName[categoryName] ?: return@forEach
            keywords.filter { it.lowercase() !in existingKeys }
                .forEach { keyword -> keywordRuleRepository.upsert(keyword, categoryId) }
        }
    }

    private suspend fun seedTaxonomyTaxonomy() {
        val existingKeys = taxonomyKeywordRuleRepository.getAll().map { it.keyword.lowercase() }.toSet()

        taxonomyTaxonomy.forEach { (taxonomy, keywords) ->
            keywords.filter { it.lowercase() !in existingKeys }
                .forEach { keyword -> taxonomyKeywordRuleRepository.upsert(keyword, taxonomy) }
        }
    }
}
