package com.budgetmanager.app.sms

/**
 * Decides whether an SMS looks like a bank debit worth keeping. Deliberately simple and
 * bank-agnostic for now: the whole point of splitting the SMS work is that receiving and
 * storing needs no sample messages, so it can start before real samples are collected.
 * Per-bank amount and merchant extraction is SmsParser, once samples exist to test against
 * (see 14-implementation-plan.md M2b and 04-messages-and-notifications.md).
 */
object SpendClassifier {

    private val debitRegex = Regex(
        """\b(debited|spent|withdrawn|purchase|paid)\b""",
        RegexOption.IGNORE_CASE
    )
    private val debitedRegex = Regex("""\bdebited\b""", RegexOption.IGNORE_CASE)
    private val otpRegex = Regex("""\botp\b|one[- ]time password""", RegexOption.IGNORE_CASE)
    // Autopay/due-date pre-notices ("will be debited for Rs X on <future date>") describe a
    // transaction that hasn't happened yet - keeping them created a false duplicate alongside
    // the real completed-debit SMS that follows later. Confirmed against real ICICI autopay and
    // Policybazaar due-payment messages.
    private val futureTenseRegex = Regex("""\bwill be debited\b""", RegexOption.IGNORE_CASE)

    /**
     * A message needs a debit word to be kept. The amount is no longer required - it's still
     * parsed and shown whenever SmsParser finds one, but its absence no longer excludes the
     * message. A real gap found 2026-10-07: a genuine mutual fund purchase confirmation
     * ("Systematic Investment Purchase...has been processed") never states an amount inline,
     * pointing to a separate statement instead - requiring an amount was silently dropping real
     * transactions like it, with no trace anywhere in the app. The keyword check alone already
     * correctly identified that message as transaction-shaped ("Purchase" matched); it was
     * specifically the amount requirement doing the harm. OTPs are never stored (R13, R14).
     */
    fun isSpendLike(body: String): Boolean {
        if (!debitRegex.containsMatchIn(body)) return false
        if (futureTenseRegex.containsMatchIn(body)) return false
        // A message that mentions OTP but also says "debited" is a real debit alert
        // ("do not share OTP" is common wording in genuine bank debit SMS).
        if (otpRegex.containsMatchIn(body) && !debitedRegex.containsMatchIn(body)) return false
        return true
    }
}
