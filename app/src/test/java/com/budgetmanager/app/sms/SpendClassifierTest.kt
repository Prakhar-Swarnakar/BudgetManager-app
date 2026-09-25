package com.budgetmanager.app.sms

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpendClassifierTest {

    @Test
    fun `a UPI debit-to-person message is spend-like`() {
        val body = "ICICI Bank Acct XX756 debited for Rs 282.00 on 18-Sep-26; TEST PERSON credited. UPI:662745660770. Call 18002662 for dispute."
        assertTrue(SpendClassifier.isSpendLike(body))
    }

    @Test
    fun `a credit card spend message is spend-like`() {
        val body = "Rs.775.00 spent on your Test Bank Credit Card ending with 5590 at BLINKIT on 18-09-26 via UPI (Ref No. 662717134848)."
        assertTrue(SpendClassifier.isSpendLike(body))
    }

    @Test
    fun `an autopay pre-notice - will be debited - is not spend-like`() {
        // Confirmed against a real ICICI autopay pre-notice: it arrives before the actual debit
        // SMS and would otherwise create a false duplicate.
        val body = "ICICI Bank SAVINGS Account XX756 will be debited for Rs 6357.00 on 21-Sep-26 towards Autopay for Test Vendor, Unique Mandate Number abc123@okaxis"
        assertFalse(SpendClassifier.isSpendLike(body))
    }

    @Test
    fun `a due-payment reminder - will be debited - is not spend-like`() {
        val body = "Payment of Rs.6357 for your Test Life policy is due on 2026-09-21. The amount will be debited from your credit card/bank account."
        assertFalse(SpendClassifier.isSpendLike(body))
    }

    @Test
    fun `a credit reversal is not spend-like`() {
        val body = "Rs.152.99 has been credited to your credit card ending with 5590 as reversal of UPI trxn. dated 23-09-26."
        assertFalse(SpendClassifier.isSpendLike(body))
    }

    @Test
    fun `an OTP is not spend-like`() {
        assertFalse(SpendClassifier.isSpendLike("Your OTP is 123456. Do not share it with anyone."))
    }

    @Test
    fun `a genuine debit alert that also warns about OTP sharing is still spend-like`() {
        val body = "Rs 500 debited from a/c XX1234. Do not share your OTP with anyone, including bank staff."
        assertTrue(SpendClassifier.isSpendLike(body))
    }

    @Test
    fun `a message with no amount is not spend-like`() {
        assertFalse(SpendClassifier.isSpendLike("Your account statement is ready to view."))
    }

    @Test
    fun `a message with an amount but no debit word is not spend-like`() {
        assertFalse(SpendClassifier.isSpendLike("Recharge of INR 99.00 is successful for your mobile."))
    }
}
