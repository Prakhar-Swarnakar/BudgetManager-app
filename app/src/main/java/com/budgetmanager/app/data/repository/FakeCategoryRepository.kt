package com.budgetmanager.app.data.repository

import com.budgetmanager.app.core.model.Category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory fake for ViewModel tests. Behaves like the real repository, without Room. */
class FakeCategoryRepository : CategoryRepository {
    private val state = MutableStateFlow<List<Category>>(emptyList())
    private var nextId = 1L

    override fun observeAll() = state.map { it.sortedBy { c -> c.sortOrder } }
    override fun observeActive() = state.map { list -> list.filter { !it.archived }.sortedBy { it.sortOrder } }
    override suspend fun getById(id: Long) = state.value.firstOrNull { it.id == id }

    override suspend fun create(name: String, emoji: String): Long {
        val id = nextId++
        state.value = state.value + Category(id, name, emoji, sortOrder = state.value.size, archived = false)
        return id
    }

    /** Test helper: seed the fake directly, bypassing create(). */
    fun seed(categories: List<Category>) {
        state.value = categories
    }
}
