package com.budgetmanager.app.data.repository

import com.budgetmanager.app.core.model.KeywordRule
import kotlinx.coroutines.flow.Flow

interface KeywordRuleRepository {
    suspend fun getAll(): List<KeywordRule>

    /** For the editable-rules screen - reacts to both manual edits and auto-learned rules. */
    fun observeAll(): Flow<List<KeywordRule>>

    /** Replaces any existing rule for [keyword] - used both when the user edits a rule directly
     *  and when a transaction save learns one from the category the user actually picked. */
    suspend fun upsert(keyword: String, categoryId: Long)

    suspend fun delete(keyword: String)
}
