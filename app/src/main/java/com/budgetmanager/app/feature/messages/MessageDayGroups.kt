package com.budgetmanager.app.feature.messages

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** One day's rows under one sticky header. [outPaise] is your share of that day's Confirmed
 *  spending - 0 when there is none. */
data class DayGroup(val title: String, val rows: List<MessageRowUi>, val outPaise: Long)

private val dayFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)

/** Groups [rows] (already newest first) by calendar day: "Today", "Yesterday", then "Thu 19 Nov".
 *  Days with no messages get no group. The month selector keeps the list inside one month, so
 *  the date needs no year. */
fun groupByDay(rows: List<MessageRowUi>, today: LocalDate, zone: ZoneId): List<DayGroup> =
    rows.groupBy { it.receivedAt.atZone(zone).toLocalDate() }
        .map { (date, dayRows) ->
            DayGroup(
                title = when (date) {
                    today -> "Today"
                    today.minusDays(1) -> "Yesterday"
                    else -> date.format(dayFormatter)
                },
                rows = dayRows,
                outPaise = dayRows.sumOf { it.spendPaise }
            )
        }
