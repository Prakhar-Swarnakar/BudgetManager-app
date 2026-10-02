package com.budgetmanager.app.domain

import com.budgetmanager.app.core.model.Category
import com.budgetmanager.app.core.model.KeywordRule
import com.budgetmanager.app.core.model.TaxonomyRule
import com.budgetmanager.app.core.model.TaxonomyType
import com.budgetmanager.app.data.repository.FakeCategoryRepository
import com.budgetmanager.app.data.repository.FakeKeywordRuleRepository
import com.budgetmanager.app.data.repository.FakeSettingsRepository
import com.budgetmanager.app.data.repository.FakeTaxonomyKeywordRuleRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedKeywordTaxonomyTest {

    private val starterCategoryNames = listOf(
        "Food & Dining", "Groceries", "Transport", "Entertainment", "Shopping", "Bills & Utilities", "Health"
    )

    private fun categories(names: List<String> = starterCategoryNames) = FakeCategoryRepository().apply {
        seed(names.mapIndexed { index, name -> Category(index + 1L, name, "📦", index, false) })
    }

    @Test
    fun `seeds category-merchant keywords for categories matching starter names`() = runTest {
        val categoryRepo = categories()
        val keywordRules = FakeKeywordRuleRepository()
        val seed = SeedKeywordTaxonomy(
            keywordRules, FakeTaxonomyKeywordRuleRepository(), categoryRepo, FakeSettingsRepository()
        )

        seed()

        val foodDiningId = categoryRepo.observeActive().first().single { it.name == "Food & Dining" }.id
        val zomatoRule = keywordRules.getAll().single { it.keyword == "zomato" }
        assertEquals(foodDiningId, zomatoRule.categoryId)
    }

    @Test
    fun `seeds payment-method taxonomy keywords`() = runTest {
        val taxonomyRules = FakeTaxonomyKeywordRuleRepository()
        val seed = SeedKeywordTaxonomy(
            FakeKeywordRuleRepository(), taxonomyRules, categories(), FakeSettingsRepository()
        )

        seed()

        assertEquals(TaxonomyType.UPI, taxonomyRules.getAll().single { it.keyword == "upi" }.taxonomy)
        assertEquals(
            TaxonomyType.CREDIT_CARD,
            taxonomyRules.getAll().single { it.keyword == "credit card" }.taxonomy
        )
    }

    @Test
    fun `never overwrites a category keyword rule the user already set differently`() = runTest {
        val keywordRules = FakeKeywordRuleRepository().apply {
            seed(listOf(KeywordRule("zomato", categoryId = 99)))
        }
        val seed = SeedKeywordTaxonomy(
            keywordRules, FakeTaxonomyKeywordRuleRepository(), categories(), FakeSettingsRepository()
        )

        seed()

        assertEquals(99L, keywordRules.getAll().single { it.keyword == "zomato" }.categoryId)
    }

    @Test
    fun `never overwrites a taxonomy keyword rule the user already set differently`() = runTest {
        val taxonomyRules = FakeTaxonomyKeywordRuleRepository().apply {
            seed(listOf(TaxonomyRule("upi", TaxonomyType.CASH)))
        }
        val seed = SeedKeywordTaxonomy(
            FakeKeywordRuleRepository(), taxonomyRules, categories(), FakeSettingsRepository()
        )

        seed()

        assertEquals(TaxonomyType.CASH, taxonomyRules.getAll().single { it.keyword == "upi" }.taxonomy)
    }

    @Test
    fun `a second invocation does nothing once the flag is set`() = runTest {
        val keywordRules = FakeKeywordRuleRepository()
        val taxonomyRules = FakeTaxonomyKeywordRuleRepository()
        val settings = FakeSettingsRepository()
        val seed = SeedKeywordTaxonomy(keywordRules, taxonomyRules, categories(), settings)

        seed()
        val keywordCountAfterFirstRun = keywordRules.getAll().size
        val taxonomyCountAfterFirstRun = taxonomyRules.getAll().size

        // A rule the user added themselves between invocations must survive untouched.
        keywordRules.upsert("mymanualrule", categoryId = 1)

        seed()

        assertEquals(keywordCountAfterFirstRun + 1, keywordRules.getAll().size)
        assertEquals(taxonomyCountAfterFirstRun, taxonomyRules.getAll().size)
    }

    @Test
    fun `a missing category's keywords are skipped without crashing or creating a category`() = runTest {
        val categoryRepo = categories(starterCategoryNames - "Health")
        val keywordRules = FakeKeywordRuleRepository()
        val seed = SeedKeywordTaxonomy(
            keywordRules, FakeTaxonomyKeywordRuleRepository(), categoryRepo, FakeSettingsRepository()
        )

        seed()

        assertTrue(keywordRules.getAll().none { it.keyword == "1mg" })
        val activeCategories = categoryRepo.observeActive().first()
        assertNull(activeCategories.firstOrNull { it.name == "Health" })
        assertEquals(starterCategoryNames.size - 1, activeCategories.size)
    }
}
