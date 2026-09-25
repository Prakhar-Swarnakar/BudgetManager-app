package com.budgetmanager.app.data.repository

import com.budgetmanager.app.core.model.Category
import com.budgetmanager.app.data.database.dao.CategoryDao
import com.budgetmanager.app.data.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomCategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun observeAll(): Flow<List<Category>> =
        categoryDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeActive(): Flow<List<Category>> =
        categoryDao.observeActive().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getById(id: Long): Category? = categoryDao.getById(id)?.toDomain()

    override suspend fun create(name: String, emoji: String): Long {
        val nextSortOrder = categoryDao.count()
        return categoryDao.insert(CategoryEntity(name = name, emoji = emoji, sortOrder = nextSortOrder))
    }

    override suspend fun update(id: Long, name: String, emoji: String) {
        val existing = categoryDao.getById(id) ?: return
        categoryDao.update(existing.copy(name = name, emoji = emoji))
    }

    override suspend fun reorder(orderedActiveIds: List<Long>) {
        val current = categoryDao.observeAll().first()
        val byId = current.associateBy { it.id }
        val archivedInOrder = current.filter { it.archived }.sortedBy { it.sortOrder }
        val updated = buildList {
            orderedActiveIds.forEachIndexed { index, id ->
                byId[id]?.let { add(it.copy(sortOrder = index)) }
            }
            archivedInOrder.forEachIndexed { index, entity ->
                add(entity.copy(sortOrder = orderedActiveIds.size + index))
            }
        }
        categoryDao.updateAll(updated)
    }
}

private fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    emoji = emoji,
    sortOrder = sortOrder,
    archived = archived
)
