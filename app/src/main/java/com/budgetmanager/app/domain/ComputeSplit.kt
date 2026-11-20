package com.budgetmanager.app.domain

import com.budgetmanager.app.core.model.Money

/** One friend in a split. A null [customAmount] means "split this one equally" among all other
 *  null-amount entries, after everyone else's explicit custom amounts are set aside. */
data class SplitFriendInput(val name: String, val customAmount: Money? = null)

data class SplitFriendResult(val name: String, val amountOwed: Money)

/** [yourShare] can go negative if the custom amounts entered add up to more than [totalAmount] -
 *  ComputeSplit never rejects that, it just computes the arithmetic; the caller validates and
 *  shows an error before letting the user save. */
data class SplitResult(
    val friends: List<SplitFriendResult>,
    val yourShare: Money,
    val owedTotal: Money
)

/**
 * Splits a transaction's total amount among friends plus yourself (06-backlog.md, "Split with
 * friends"). Your share is always totalAmount minus everyone else's - never recomputed any other
 * way, so it can't drift from what the split actually adds up to.
 */
object ComputeSplit {

    fun compute(totalAmount: Money, friends: List<SplitFriendInput>): SplitResult {
        if (friends.isEmpty()) return SplitResult(emptyList(), totalAmount, Money(0))

        val customTotal = friends.sumOf { it.customAmount?.paise ?: 0 }
        // Indices rather than names: two friends can share the same name, and a map keyed by
        // name would silently merge their equal-split remainder allocation.
        val equalIndices = friends.indices.filter { friends[it].customAmount == null }
        val remainingForEqualSplit = (totalAmount.paise - customTotal).coerceAtLeast(0)
        // You're one of the equal participants too - an equal split among 2 friends is a 3-way
        // split (you + both of them), not 2-way with you taking whatever's left over.
        val equalParticipants = equalIndices.size + 1
        val base = remainingForEqualSplit / equalParticipants
        // An equal split that doesn't divide evenly gives the leftover paise, one each, to the
        // first few equally-split friends in list order, never to you - the total always
        // reconciles exactly, and remainder is always <= equalIndices.size by construction
        // (it's strictly less than equalParticipants = equalIndices.size + 1).
        val remainder = (remainingForEqualSplit % equalParticipants).toInt()

        val results = friends.mapIndexed { index, entry ->
            val amount = entry.customAmount?.paise ?: run {
                val positionAmongEqual = equalIndices.indexOf(index)
                base + if (positionAmongEqual < remainder) 1 else 0
            }
            SplitFriendResult(entry.name, Money(amount))
        }
        val owedTotal = results.sumOf { it.amountOwed.paise }
        return SplitResult(results, Money(totalAmount.paise - owedTotal), Money(owedTotal))
    }
}
