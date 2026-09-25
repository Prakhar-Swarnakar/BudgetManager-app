package com.budgetmanager.app.sms

import com.budgetmanager.app.core.model.KeywordRule
import com.budgetmanager.app.data.repository.FakeKeywordRuleRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategorySuggesterTest {

    private fun suggester(vararg rules: KeywordRule): CategorySuggester {
        val repo = FakeKeywordRuleRepository().apply { seed(rules.toList()) }
        return CategorySuggester(repo)
    }

    @Test
    fun `a matching keyword suggests its category, case-insensitively`() = runTest {
        val suggester = suggester(KeywordRule("blinkit", categoryId = 2))
        assertEquals(2L, suggester.suggest("BLINKIT"))
    }

    @Test
    fun `no matching keyword suggests nothing`() = runTest {
        val suggester = suggester(KeywordRule("swiggy", categoryId = 3))
        assertNull(suggester.suggest("Some Unrelated Store"))
    }

    @Test
    fun `the longest matching keyword wins over a shorter substring match`() = runTest {
        val suggester = suggester(
            KeywordRule("mart", categoryId = 1),
            KeywordRule("luluvaluemart", categoryId = 2)
        )
        assertEquals(2L, suggester.suggest("LULUVALUEMARTC3"))
    }
}
