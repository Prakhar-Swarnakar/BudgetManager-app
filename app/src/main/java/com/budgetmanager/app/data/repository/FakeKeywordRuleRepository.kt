package com.budgetmanager.app.data.repository

import com.budgetmanager.app.core.model.KeywordRule

class FakeKeywordRuleRepository : KeywordRuleRepository {
    private var rules: List<KeywordRule> = emptyList()

    override suspend fun getAll(): List<KeywordRule> = rules

    fun seed(newRules: List<KeywordRule>) {
        rules = newRules
    }
}
