package com.budgetmanager.app.data.repository

import com.budgetmanager.app.core.model.KeywordRule

interface KeywordRuleRepository {
    suspend fun getAll(): List<KeywordRule>
}
