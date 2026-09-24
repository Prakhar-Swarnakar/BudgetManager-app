package com.budgetmanager.app.core.model

import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/**
 * A calendar month, such as "2026-09". A transaction belongs to the month of its date
 * (taken from the SMS timestamp, or the date entered manually), not the month it was
 * entered on the phone. Budgets are compared only against their own month - no rollover.
 */
@JvmInline
value class MonthKey(val value: String) : Comparable<MonthKey> {

    init {
        require(FORMAT_REGEX.matches(value)) { "Invalid month key: $value" }
    }

    val year: Int get() = value.substring(0, 4).toInt()
    val month: Int get() = value.substring(5, 7).toInt()

    fun previous(): MonthKey = toYearMonth().minusMonths(1).toMonthKey()
    fun next(): MonthKey = toYearMonth().plusMonths(1).toMonthKey()

    private fun toYearMonth(): YearMonth = YearMonth.of(year, month)

    override fun compareTo(other: MonthKey): Int = value.compareTo(other.value)

    override fun toString(): String = value

    companion object {
        private val FORMAT_REGEX = Regex("""\d{4}-\d{2}""")

        fun of(year: Int, month: Int): MonthKey = MonthKey("%04d-%02d".format(year, month))

        /** The month a timestamp falls in, in the given time zone (the phone's zone in real use). */
        fun from(instant: Instant, zone: ZoneId): MonthKey {
            val date = instant.atZone(zone).toLocalDate()
            return of(date.year, date.monthValue)
        }

        /** The current month. Tests must pass a fixed [clock] rather than relying on this default,
         *  so results never depend on the day the test happens to run. */
        fun current(clock: Clock = Clock.systemDefaultZone()): MonthKey =
            from(Instant.now(clock), clock.zone)

        private fun YearMonth.toMonthKey(): MonthKey = of(year, monthValue)
    }
}
