package com.budgetmanager.app.data.repository

import com.budgetmanager.app.core.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeAll(): Flow<List<Category>>
    fun observeActive(): Flow<List<Category>>
    suspend fun getById(id: Long): Category?

    /** Appends a new category after the existing ones. */
    suspend fun create(name: String, emoji: String): Long
}
