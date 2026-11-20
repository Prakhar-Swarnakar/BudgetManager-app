package com.budgetmanager.app.sms

/**
 * Decides whether an SMS looks like money coming in. Deliberately generic for v1 (06-backlog.md,
 * "Credit handling") - no Refund/Settlement/Income sub-typing, just "this is incoming money,
 * worth a row". Must only ever be consulted after SpendClassifier.isSpendLike has already said
 * no: a real debit SMS often contains the word "credited" describing its payee (e.g.
 * "...debited for Rs 282.00...; MANAN MANCHANDA credited...UPI:..."), and checking debit-likeness
 * first is what keeps that from being misfiled as a credit.
 */
object CreditClassifier {

    private val creditRegex = Regex(
        """\b(credited|received|deposited)\b""",
        RegexOption.IGNORE_CASE
    )
    private val creditedRegex = Regex("""\bcredited\b""", RegexOption.IGNORE_CASE)
    private val otpRegex = Regex("""\botp\b|one[- ]time password""", RegexOption.IGNORE_CASE)
    // Mirrors SpendClassifier's own "will be debited" guard - a pre-notice describing money that
    // hasn't arrived yet ("will be credited on <date>") is not a completed credit. This exact
    // asymmetry (debit side had the guard, credit side didn't) was found 2026-10-07 while fixing
    // the amount requirement below, not from a real reported message - no confirmed real "will be
    // credited" pre-notice has turned up yet, but the risk is the same shape as the debit one.
    private val futureTenseRegex = Regex("""\bwill be credited\b""", RegexOption.IGNORE_CASE)

    /**
     * [body] must already have failed SpendClassifier.isSpendLike - callers are responsible for
     * that ordering (see SmsReceiver/DefaultInboxScanner), it is deliberately not re-checked
     * here so this stays a pure "does this look like incoming money" question on its own.
     *
     * The amount is no longer required, mirroring [SpendClassifier.isSpendLike]'s same fix - it's
     * still parsed and shown whenever one is found, but its absence no longer excludes the
     * message.
     */
    fun isCreditLike(body: String): Boolean {
        if (!creditRegex.containsMatchIn(body)) return false
        if (futureTenseRegex.containsMatchIn(body)) return false
        // Same OTP carve-out shape as SpendClassifier: a genuine credit alert that also warns
        // "don't share your OTP" is common bank boilerplate, not an actual OTP message.
        if (otpRegex.containsMatchIn(body) && !creditedRegex.containsMatchIn(body)) return false
        return true
    }
}
