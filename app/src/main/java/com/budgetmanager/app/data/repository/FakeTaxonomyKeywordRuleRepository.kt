package com.budgetmanager.app.data.repository

import com.budgetmanager.app.core.model.TaxonomyRule
import com.budgetmanager.app.core.model.TaxonomyType
import kotlinx.coroutines.flow.MutableStateFlow

class FakeTaxonomyKeywordRuleRepository : TaxonomyKeywordRuleRepository {
    private val rules = MutableStateFlow<List<TaxonomyRule>>(emptyList())

    override suspend fun getAll(): List<TaxonomyRule> = rules.value

    override fun observeAll() = rules

    override suspend fun upsert(keyword: String, taxonomy: TaxonomyType) {
        rules.value = rules.value.filterNot { it.keyword == keyword } + TaxonomyRule(keyword, taxonomy)
    }

    override suspend fun delete(keyword: String) {
        rules.value = rules.value.filterNot { it.keyword == keyword }
    }

    /** Test helper: seed the fake directly, bypassing upsert(). */
    fun seed(newRules: List<TaxonomyRule>) {
        rules.value = newRules
    }
}
