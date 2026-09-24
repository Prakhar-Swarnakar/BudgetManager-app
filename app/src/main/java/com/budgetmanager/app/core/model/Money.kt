package com.budgetmanager.app.core.model

/**
 * An amount of money, stored as whole paise (1 rupee = 100 paise) so nothing is ever
 * rounded by floating point. Never use Double or Float for money anywhere in the app -
 * convert only when formatting for display.
 */
@JvmInline
value class Money(val paise: Long) : Comparable<Money> {

    operator fun plus(other: Money): Money = Money(paise + other.paise)
    operator fun minus(other: Money): Money = Money(paise - other.paise)
    operator fun unaryMinus(): Money = Money(-paise)

    override fun compareTo(other: Money): Int = paise.compareTo(other.paise)

    /** Formats as "₹1,25,000", "-₹2,000", or "₹0", using Indian digit grouping. Paise are shown
     *  only when non-zero, e.g. "₹250.50". This is the only place money is formatted. */
    fun formatted(): String = formatPaise(paise)

    companion object {
        val Zero = Money(0)

        /** For known-valid domain amounts (e.g. test fixtures, a computed remaining balance,
         *  which may be negative). Not for user input - see [parseRupeeInput]. */
        fun ofRupees(rupees: Long): Money = Money(rupees * 100)

        /**
         * Strictly parses a user-entered rupee amount, such as "1250" or "1250.50".
         * Returns null for empty, zero, negative, non-numeric, or more than 2 decimal places -
         * callers must reject those before saving, never silently coerce them.
         */
        fun parseRupeeInput(input: String): Money? {
            val trimmed = input.trim()
            if (trimmed.isEmpty()) return null
            val match = RUPEE_INPUT_REGEX.matchEntire(trimmed) ?: return null
            val rupees = match.groupValues[1].toLongOrNull() ?: return null
            val paisePart = match.groupValues[2]
            val paise = when (paisePart.length) {
                0 -> 0L
                1 -> paisePart.toLong() * 10
                else -> paisePart.toLong()
            }
            val total = rupees * 100 + paise
            return if (total <= 0) null else Money(total)
        }

        /** Parses an amount extracted from bank SMS text, such as "1,250.00" or "500". */
        fun parseSmsAmount(raw: String): Money? = parseRupeeInput(raw.replace(",", ""))

        private val RUPEE_INPUT_REGEX = Regex("""(\d+)(?:\.(\d{1,2}))?""")

        private fun formatPaise(paise: Long): String {
            val negative = paise < 0
            val absPaise = kotlin.math.abs(paise)
            val rupees = absPaise / 100
            val remainderPaise = absPaise % 100
            val decimalPart = if (remainderPaise == 0L) "" else ".%02d".format(remainderPaise)
            val sign = if (negative) "-" else ""
            return "$sign₹${groupIndian(rupees)}$decimalPart"
        }

        /** Groups a non-negative integer using the Indian numbering system: last 3 digits, then pairs. */
        private fun groupIndian(value: Long): String {
            val digits = value.toString()
            if (digits.length <= 3) return digits
            val lastThree = digits.substring(digits.length - 3)
            val rest = digits.substring(0, digits.length - 3)
            val grouped = StringBuilder()
            var i = rest.length
            while (i > 2) {
                grouped.insert(0, rest.substring(i - 2, i))
                grouped.insert(0, ",")
                i -= 2
            }
            grouped.insert(0, rest.substring(0, i))
            return "$grouped,$lastThree"
        }
    }
}
