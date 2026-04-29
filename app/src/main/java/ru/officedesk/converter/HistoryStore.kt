package ru.officedesk.converter

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object HistoryStore {
    private const val PREF = "history"
    private const val KEY = "items"

    fun load(ctx: Context): List<HistoryItem> {
        val raw = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        HistoryItem(
                            timestamp = o.getLong("ts"),
                            sourceName = o.getString("src"),
                            from = o.getString("from"),
                            to = o.getString("to"),
                            outputUri = o.getString("uri"),
                            outputName = o.getString("name"),
                        )
                    )
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun save(ctx: Context, items: List<HistoryItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject()
                    .put("ts", item.timestamp)
                    .put("src", item.sourceName)
                    .put("from", item.from)
                    .put("to", item.to)
                    .put("uri", item.outputUri)
                    .put("name", item.outputName)
            )
        }
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString(KEY, arr.toString())
            .apply()
    }
}
