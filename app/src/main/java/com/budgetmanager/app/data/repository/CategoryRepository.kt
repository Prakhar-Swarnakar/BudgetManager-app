package com.budgetmanager.app.data.repository

import com.budgetmanager.app.core.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeAll(): Flow<List<Category>>
    fun observeActive(): Flow<List<Category>>
    suspend fun getById(id: Long): Category?

    /** Appends a new category after the existing ones. */
    suspend fun create(name: String, emoji: String): Long

    suspend fun update(id: Long, name: String, emoji: String)

    /** Assigns sort order 0..n-1 to [orderedActiveIds] in that order. Archived categories keep
     *  their existing relative order, renumbered to continue right after the active ones. */
    suspend fun reorder(orderedActiveIds: List<Long>)
}
