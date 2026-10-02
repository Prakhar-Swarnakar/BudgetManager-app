package com.budgetmanager.app.core.model

/**
 * How a spend was paid for - a fixed, closed set (06-backlog.md, "Payment method taxonomy"),
 * unlike Category which the user can create/rename/reorder freely. Stored by Room as its own
 * enum constant name (TEXT column), the same mechanism already used for MessageStatus.
 */
enum class TaxonomyType(val label: String) {
    UPI("UPI"),
    CREDIT_CARD("Credit Card"),
    DEBIT_CARD("Debit Card"),
    /** A RuPay credit card linked to GPay/PhonePe etc. - the SMS often just says "UPI" even
     *  though a credit card funded it, so this is its own value rather than folded into UPI. */
    CREDIT_CARD_VIA_UPI("Credit Card via UPI"),
    BANK_TRANSFER("Bank Transfer"),
    WALLET("Wallet"),
    CASH("Cash"),
    OTHER("Other")
}
