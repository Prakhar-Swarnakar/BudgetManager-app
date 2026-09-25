package com.budgetmanager.app.data.repository

import com.budgetmanager.app.core.model.Category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory fake for ViewModel tests. Behaves like the real repository, without Room. */
class FakeCategoryRepository : CategoryRepository {
    private val state = MutableStateFlow<List<Category>>(emptyList())

    override fun observeAll() = state.map { it.sortedBy { c -> c.sortOrder } }
    override fun observeActive() = state.map { list -> list.filter { !it.archived }.sortedBy { it.sortOrder } }
    override suspend fun getById(id: Long) = state.value.firstOrNull { it.id == id }

    override suspend fun create(name: String, emoji: String): Long {
        // Derived from current state rather than a separately-incremented counter, so a test
        // seeding explicit ids via seed() and then also calling create() can never collide.
        val id = (state.value.maxOfOrNull { it.id } ?: 0) + 1
        state.value = state.value + Category(id, name, emoji, sortOrder = state.value.size, archived = false)
        return id
    }

    override suspend fun update(id: Long, name: String, emoji: String) {
        state.value = state.value.map { if (it.id == id) it.copy(name = name, emoji = emoji) else it }
    }

    override suspend fun setArchived(id: Long, archived: Boolean) {
        state.value = state.value.map { if (it.id == id) it.copy(archived = archived) else it }
    }

    override suspend fun reorder(orderedActiveIds: List<Long>) {
        val byId = state.value.associateBy { it.id }
        val archivedInOrder = state.value.filter { it.archived }.sortedBy { it.sortOrder }
        val updated = buildList {
            orderedActiveIds.forEachIndexed { index, id ->
                byId[id]?.let { add(it.copy(sortOrder = index)) }
            }
            archivedInOrder.forEachIndexed { index, entity ->
                add(entity.copy(sortOrder = orderedActiveIds.size + index))
            }
        }
        state.value = updated
    }

    /** Test helper: seed the fake directly, bypassing create(). */
    fun seed(categories: List<Category>) {
        state.value = categories
    }
}
