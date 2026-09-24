package com.example.budgetspike

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** One SMS caught by the receiver. */
data class LogEntry(
    val sender: String,
    /** When our receiver ran (phone clock, ms). */
    val receivedAt: Long,
    /** The SMS's own timestamp (ms). The gap between the two shows delivery delay. */
    val smsTimestamp: Long,
    val spendLike: Boolean,
    val amount: String?,
    /** The text is only kept for spend-like messages. Others store a placeholder. */
    val text: String,
    val seen: Boolean
)

/**
 * Tiny on-phone log of caught SMS, kept in SharedPreferences.
 * Keeps the newest 100. Uses commit() so the write is finished before the receiver ends.
 */
object SpendLog {
    private const val PREFS = "spend_log"
    private const val KEY = "entries"
    private const val MAX = 100
    private val lock = Any()

    fun add(context: Context, entry: LogEntry) {
        synchronized(lock) {
            val list = load(context).toMutableList()
            list.add(0, entry)
            save(context, list.take(MAX))
        }
    }

    fun load(context: Context): List<LogEntry> {
        synchronized(lock) {
            val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY, null) ?: return emptyList()
            return try {
                val arr = JSONArray(raw)
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    LogEntry(
                        sender = o.optString("sender"),
                        receivedAt = o.optLong("receivedAt"),
                        smsTimestamp = o.optLong("smsTimestamp"),
                        spendLike = o.optBoolean("spendLike"),
                        amount = if (o.has("amount") && !o.isNull("amount")) o.getString("amount") else null,
                        text = o.optString("text"),
                        seen = o.optBoolean("seen")
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    fun unseenSpendCount(context: Context): Int =
        load(context).count { it.spendLike && !it.seen }

    fun markAllSeen(context: Context) {
        synchronized(lock) {
            val list = load(context)
            if (list.any { !it.seen }) save(context, list.map { it.copy(seen = true) })
        }
    }

    fun clear(context: Context) {
        synchronized(lock) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).commit()
        }
    }

    private fun save(context: Context, list: List<LogEntry>) {
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(
                JSONObject()
                    .put("sender", e.sender)
                    .put("receivedAt", e.receivedAt)
                    .put("smsTimestamp", e.smsTimestamp)
                    .put("spendLike", e.spendLike)
                    .put("amount", e.amount ?: JSONObject.NULL)
                    .put("text", e.text)
                    .put("seen", e.seen)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).commit()
    }
}
