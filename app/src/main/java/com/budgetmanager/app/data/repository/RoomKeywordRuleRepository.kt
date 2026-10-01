package com.budgetmanager.app.data.repository

import com.budgetmanager.app.core.model.KeywordRule
import com.budgetmanager.app.data.database.dao.KeywordRuleDao
import com.budgetmanager.app.data.database.entity.KeywordRuleEntity
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomKeywordRuleRepository @Inject constructor(
    private val keywordRuleDao: KeywordRuleDao
) : KeywordRuleRepository {

    override suspend fun getAll(): List<KeywordRule> =
        keywordRuleDao.getAll().map { KeywordRule(it.keyword, it.categoryId) }

    override fun observeAll() =
        keywordRuleDao.observeAll().map { rules -> rules.map { KeywordRule(it.keyword, it.categoryId) } }

    override suspend fun upsert(keyword: String, categoryId: Long) {
        keywordRuleDao.upsert(KeywordRuleEntity(keyword, categoryId))
    }

    override suspend fun delete(keyword: String) {
        keywordRuleDao.delete(keyword)
    }
}
