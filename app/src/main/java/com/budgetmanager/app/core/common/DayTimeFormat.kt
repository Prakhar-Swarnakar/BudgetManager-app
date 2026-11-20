package com.budgetmanager.app.core.common

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** The one "dd MMM, HH:mm" row-subtitle format, shared by every list row that shows when
 *  something happened - Messages, Owed to you, and Category detail all showed the same thing
 *  from four separate private copies of this formatter before it was pulled up here. */
private val dayTimeFormatter = DateTimeFormatter.ofPattern("dd MMM, HH:mm")

/** e.g. "07 Oct, 14:32", in the given zone (the phone's own zone in real use). */
fun Instant.toDayTimeText(zone: ZoneId = ZoneId.systemDefault()): String =
    atZone(zone).format(dayTimeFormatter)
