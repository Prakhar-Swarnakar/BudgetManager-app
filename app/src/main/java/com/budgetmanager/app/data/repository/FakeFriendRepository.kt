package com.budgetmanager.app.data.repository

import com.budgetmanager.app.core.model.Friend
import kotlinx.coroutines.flow.MutableStateFlow

class FakeFriendRepository : FriendRepository {
    private val state = MutableStateFlow<List<Friend>>(emptyList())
    private var nextId = 1L

    /** Synchronous peek at the current list, for other fakes to join against (e.g.
     *  FakeTransactionSplitRepository.observeOwedToYou) without a suspend call. */
    val current: List<Friend> get() = state.value

    override fun observeAll() = state

    override suspend fun getOrCreate(name: String): Long {
        val trimmed = name.trim()
        state.value.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }?.let { return it.id }
        val id = nextId++
        state.value = state.value + Friend(id, trimmed)
        return id
    }

    /** Test helper: seed the fake directly, bypassing getOrCreate(). */
    fun seed(friends: List<Friend>) {
        state.value = friends
    }
}
