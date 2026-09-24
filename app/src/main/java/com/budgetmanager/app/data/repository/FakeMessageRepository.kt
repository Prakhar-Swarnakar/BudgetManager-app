package com.budgetmanager.app.data.repository

import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.core.model.SmsMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeMessageRepository : MessageRepository {
    private val state = MutableStateFlow<List<SmsMessage>>(emptyList())
    private var nextId = 1L

    override fun observeAll() = state.map { it.sortedByDescending { m -> m.receivedAt } }
    override fun observeByStatus(status: MessageStatus) = state.map { list -> list.filter { it.status == status } }
    override fun observeCountByStatus(status: MessageStatus) = state.map { list -> list.count { it.status == status } }
    override fun observeNewCount() = state.map { list -> list.count { it.isNew } }
    override suspend fun getById(id: Long) = state.value.firstOrNull { it.id == id }

    override suspend fun ingest(message: SmsMessage): Long? {
        if (state.value.any { it.dedupeKey == message.dedupeKey }) return null
        val id = nextId++
        state.value = state.value + message.copy(id = id)
        return id
    }

    override suspend fun reject(id: Long) {
        updateStatus(id, MessageStatus.REJECTED)
    }

    override suspend fun markAllSeen() {
        state.value = state.value.map { it.copy(isNew = false) }
    }

    /** Test helper, also used by FakeTransactionRepository to keep the two fakes in sync. */
    fun updateStatus(id: Long, status: MessageStatus) {
        state.value = state.value.map { if (it.id == id) it.copy(status = status) else it }
    }
}
