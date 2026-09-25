package com.budgetmanager.app.data.repository

import com.budgetmanager.app.core.model.KeywordRule
import com.budgetmanager.app.data.database.dao.KeywordRuleDao
import javax.inject.Inject

class RoomKeywordRuleRepository @Inject constructor(
    private val keywordRuleDao: KeywordRuleDao
) : KeywordRuleRepository {

    override suspend fun getAll(): List<KeywordRule> =
        keywordRuleDao.getAll().map { KeywordRule(it.keyword, it.categoryId) }
}
