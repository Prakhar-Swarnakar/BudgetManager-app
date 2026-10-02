package com.budgetmanager.app.sms

import com.budgetmanager.app.core.model.TaxonomyRule
import com.budgetmanager.app.core.model.TaxonomyType
import com.budgetmanager.app.data.repository.FakeTaxonomyKeywordRuleRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaxonomySuggesterTest {

    private fun suggester(vararg rules: TaxonomyRule): TaxonomySuggester {
        val repo = FakeTaxonomyKeywordRuleRepository().apply { seed(rules.toList()) }
        return TaxonomySuggester(repo)
    }

    @Test
    fun `a matching keyword suggests its taxonomy, case-insensitively`() = runTest {
        val suggester = suggester(TaxonomyRule("gpay", TaxonomyType.UPI))
        assertEquals(TaxonomyType.UPI, suggester.suggest("GPAY"))
    }

    @Test
    fun `no matching keyword suggests nothing`() = runTest {
        val suggester = suggester(TaxonomyRule("credit card", TaxonomyType.CREDIT_CARD))
        assertNull(suggester.suggest("Some Unrelated Text"))
    }

    @Test
    fun `the longest matching keyword wins over a shorter substring match`() = runTest {
        val suggester = suggester(
            TaxonomyRule("card", TaxonomyType.DEBIT_CARD),
            TaxonomyRule("credit card", TaxonomyType.CREDIT_CARD)
        )
        assertEquals(TaxonomyType.CREDIT_CARD, suggester.suggest("Paid by credit card"))
    }
}
