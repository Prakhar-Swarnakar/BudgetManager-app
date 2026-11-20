package com.budgetmanager.app.domain

import com.budgetmanager.app.core.model.Money
import org.junit.Assert.assertEquals
import org.junit.Test

class ComputeSplitTest {

    @Test
    fun `no friends means your share is the whole amount`() {
        val result = ComputeSplit.compute(Money.ofRupees(500), emptyList())
        assertEquals(Money.ofRupees(500), result.yourShare)
        assertEquals(Money(0), result.owedTotal)
        assertEquals(emptyList<SplitFriendResult>(), result.friends)
    }

    @Test
    fun `an equal split among 2 friends plus you divides evenly when it divides evenly`() {
        val result = ComputeSplit.compute(
            Money.ofRupees(300),
            listOf(SplitFriendInput("Raj"), SplitFriendInput("Priya"))
        )
        assertEquals(Money.ofRupees(100), result.yourShare)
        assertEquals(Money.ofRupees(100), result.friends[0].amountOwed)
        assertEquals(Money.ofRupees(100), result.friends[1].amountOwed)
    }

    @Test
    fun `an equal split that does not divide evenly gives the leftover paisa to friends, never to you`() {
        // Rs 100 among you + 2 friends = 3-way split = 33.33... each - 1 remainder paisa after
        // flooring, going to the first friend in list order, never to you.
        val result = ComputeSplit.compute(
            Money.ofRupees(100),
            listOf(SplitFriendInput("Raj"), SplitFriendInput("Priya"))
        )
        assertEquals(3334L, result.friends[0].amountOwed.paise)
        assertEquals(3333L, result.friends[1].amountOwed.paise)
        assertEquals(3333L, result.yourShare.paise)
        val total = result.yourShare.paise + result.friends.sumOf { it.amountOwed.paise }
        assertEquals(10000L, total) // reconciles exactly, no paisa lost or gained
    }

    @Test
    fun `a custom amount is taken exactly, and your share is whatever is left`() {
        val result = ComputeSplit.compute(
            Money.ofRupees(500),
            listOf(SplitFriendInput("Raj", customAmount = Money.ofRupees(200)))
        )
        assertEquals(Money.ofRupees(200), result.friends[0].amountOwed)
        assertEquals(Money.ofRupees(300), result.yourShare)
    }

    @Test
    fun `mixing a custom amount with an equal split splits only what's left, among you and the equal friends`() {
        val result = ComputeSplit.compute(
            Money.ofRupees(500),
            listOf(
                SplitFriendInput("Raj", customAmount = Money.ofRupees(200)),
                SplitFriendInput("Priya"), // equal share of the remaining 300, among you + Priya + Amit
                SplitFriendInput("Amit")
            )
        )
        assertEquals(Money.ofRupees(200), result.friends[0].amountOwed)
        assertEquals(Money.ofRupees(100), result.friends[1].amountOwed)
        assertEquals(Money.ofRupees(100), result.friends[2].amountOwed)
        assertEquals(Money.ofRupees(100), result.yourShare)
    }

    @Test
    fun `custom amounts that add up to more than the total make your share negative, not an error`() {
        val result = ComputeSplit.compute(
            Money.ofRupees(100),
            listOf(SplitFriendInput("Raj", customAmount = Money.ofRupees(150)))
        )
        assertEquals(Money.ofRupees(-50), result.yourShare)
    }

    @Test
    fun `owedTotal is always the sum of every friend's share`() {
        val result = ComputeSplit.compute(
            Money.ofRupees(300),
            listOf(SplitFriendInput("Raj"), SplitFriendInput("Priya", customAmount = Money.ofRupees(50)))
        )
        assertEquals(result.friends.sumOf { it.amountOwed.paise }, result.owedTotal.paise)
    }
}
