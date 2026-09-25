package com.budgetmanager.app.data.repository

import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.MonthKey
import com.budgetmanager.app.core.model.SmsMessage
import com.budgetmanager.app.core.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant

/** Pass the same [FakeMessageRepository] instance a ViewModel test uses, so accepting or
 *  deleting a message-linked transaction here is reflected there too, as it would with Room. */
class FakeTransactionRepository(
    private val messages: FakeMessageRepository? = null
) : TransactionRepository {
    private val state = MutableStateFlow<List<Transaction>>(emptyList())
    private var nextId = 1L

    override fun observeForCategoryAndMonth(monthKey: MonthKey, categoryId: Long) =
        state.map { list ->
            list.filter { it.monthKey == monthKey && it.categoryId == categoryId }
                .sortedByDescending { it.occurredAt }
        }

    override fun observeSpentForCategoryAndMonth(monthKey: MonthKey, categoryId: Long) =
        state.map { list ->
            val total = list.filter { it.monthKey == monthKey && it.categoryId == categoryId }
                .sumOf { it.amount.paise }
            Money(total)
        }

    override fun observeSpentByCategoryForMonth(monthKey: MonthKey) =
        state.map { list ->
            list.filter { it.monthKey == monthKey }
                .groupBy { it.categoryId }
                .mapValues { (_, transactions) -> Money(transactions.sumOf { it.amount.paise }) }
        }

    override suspend fun getById(id: Long) = state.value.firstOrNull { it.id == id }

    override suspend fun insert(
        amount: Money,
        occurredAt: Instant,
        monthKey: MonthKey,
        categoryId: Long,
        note: String?
    ): Long {
        val id = nextId++
        state.value = state.value + Transaction(id, amount, occurredAt, monthKey, categoryId, note, null)
        return id
    }

    override suspend fun update(transaction: Transaction) {
        state.value = state.value.map { if (it.id == transaction.id) transaction else it }
    }

    override suspend fun saveFromMessage(
        message: SmsMessage,
        amount: Money,
        occurredAt: Instant,
        monthKey: MonthKey,
        categoryId: Long,
        note: String?
    ): Long {
        // Mirrors RoomTransactionRepository: update in place if a transaction is already linked
        // to this message, rather than adding a second one for the same sourceMessageId.
        val existing = state.value.firstOrNull { it.sourceMessageId == message.id }
        val id = existing?.id ?: nextId++
        val transaction = Transaction(id, amount, occurredAt, monthKey, categoryId, note, message.id)
        state.value = if (existing != null) {
            state.value.map { if (it.id == id) transaction else it }
        } else {
            state.value + transaction
        }
        messages?.updateStatus(message.id, com.budgetmanager.app.core.model.MessageStatus.ACCEPTED)
        return id
    }

    override suspend fun delete(id: Long) {
        val transaction = state.value.firstOrNull { it.id == id } ?: return
        state.value = state.value.filterNot { it.id == id }
        transaction.sourceMessageId?.let { messageId ->
            messages?.updateStatus(messageId, com.budgetmanager.app.core.model.MessageStatus.NOT_ASSIGNED)
        }
    }

    override suspend fun deleteBySourceMessage(messageId: Long) {
        val transaction = state.value.firstOrNull { it.sourceMessageId == messageId } ?: return
        state.value = state.value.filterNot { it.id == transaction.id }
        messages?.updateStatus(messageId, com.budgetmanager.app.core.model.MessageStatus.NOT_ASSIGNED)
    }
}
