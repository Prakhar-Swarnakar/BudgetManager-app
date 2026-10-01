package com.budgetmanager.app.data.repository

import com.budgetmanager.app.core.model.KeywordRule
import kotlinx.coroutines.flow.MutableStateFlow

class FakeKeywordRuleRepository : KeywordRuleRepository {
    private val rules = MutableStateFlow<List<KeywordRule>>(emptyList())

    override suspend fun getAll(): List<KeywordRule> = rules.value

    override fun observeAll() = rules

    override suspend fun upsert(keyword: String, categoryId: Long) {
        rules.value = rules.value.filterNot { it.keyword == keyword } + KeywordRule(keyword, categoryId)
    }

    override suspend fun delete(keyword: String) {
        rules.value = rules.value.filterNot { it.keyword == keyword }
    }

    /** Test helper: seed the fake directly, bypassing upsert(). */
    fun seed(newRules: List<KeywordRule>) {
        rules.value = newRules
    }
}
