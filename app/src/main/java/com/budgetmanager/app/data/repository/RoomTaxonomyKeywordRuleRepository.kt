package com.budgetmanager.app.data.repository

import com.budgetmanager.app.core.model.TaxonomyRule
import com.budgetmanager.app.core.model.TaxonomyType
import com.budgetmanager.app.data.database.dao.TaxonomyKeywordRuleDao
import com.budgetmanager.app.data.database.entity.TaxonomyKeywordRuleEntity
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomTaxonomyKeywordRuleRepository @Inject constructor(
    private val taxonomyKeywordRuleDao: TaxonomyKeywordRuleDao
) : TaxonomyKeywordRuleRepository {

    override suspend fun getAll(): List<TaxonomyRule> =
        taxonomyKeywordRuleDao.getAll().map { TaxonomyRule(it.keyword, it.taxonomy) }

    override fun observeAll() =
        taxonomyKeywordRuleDao.observeAll().map { rules -> rules.map { TaxonomyRule(it.keyword, it.taxonomy) } }

    override suspend fun upsert(keyword: String, taxonomy: TaxonomyType) {
        taxonomyKeywordRuleDao.upsert(TaxonomyKeywordRuleEntity(keyword, taxonomy))
    }

    override suspend fun delete(keyword: String) {
        taxonomyKeywordRuleDao.delete(keyword)
    }
}
