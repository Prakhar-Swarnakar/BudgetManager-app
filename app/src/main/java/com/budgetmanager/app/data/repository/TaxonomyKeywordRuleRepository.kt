package com.budgetmanager.app.data.repository

import com.budgetmanager.app.core.model.TaxonomyRule
import com.budgetmanager.app.core.model.TaxonomyType
import kotlinx.coroutines.flow.Flow

interface TaxonomyKeywordRuleRepository {
    suspend fun getAll(): List<TaxonomyRule>

    /** For the Taxonomy rules screen - reacts to both manual edits and auto-learned rules. */
    fun observeAll(): Flow<List<TaxonomyRule>>

    /** Replaces any existing rule for [keyword] - used both when the user edits a rule directly
     *  and when a transaction save learns one from the taxonomy value the user actually picked. */
    suspend fun upsert(keyword: String, taxonomy: TaxonomyType)

    suspend fun delete(keyword: String)
}
