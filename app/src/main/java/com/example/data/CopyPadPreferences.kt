package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.ClippingItem
import org.json.JSONArray
import org.json.JSONObject

class CopyPadPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("copypad_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DRAFT_TEXT = "key_draft_text"
        private const val KEY_AUTO_COPY_ENABLED = "key_auto_copy_enabled"
        private const val KEY_LAST_COPIED_TEXT = "key_last_copied_text"
        private const val KEY_LAST_COPIED_TIME = "key_last_copied_time"
        private const val KEY_LAST_COPY_SOURCE = "key_last_copy_source"
        private const val KEY_HISTORY_JSON = "key_history_json"
        private const val MAX_HISTORY_ITEMS = 20
    }

    fun saveDraftText(text: String) {
        prefs.edit().putString(KEY_DRAFT_TEXT, text).apply()
    }

    fun getDraftText(): String {
        return prefs.getString(KEY_DRAFT_TEXT, "") ?: ""
    }

    fun isAutoCopyEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_COPY_ENABLED, true)
    }

    fun setAutoCopyEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_COPY_ENABLED, enabled).apply()
    }

    fun recordCopy(text: String, source: String): ClippingItem {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putString(KEY_LAST_COPIED_TEXT, text)
            .putLong(KEY_LAST_COPIED_TIME, now)
            .putString(KEY_LAST_COPY_SOURCE, source)
            .apply()

        val newItem = ClippingItem(
            id = "${now}_${text.hashCode()}",
            text = text,
            timestamp = now,
            source = source
        )

        // Prepend to history
        val currentHistory = getHistory().toMutableList()
        // Remove duplicate text if already present at top
        currentHistory.removeAll { it.text == text }
        currentHistory.add(0, newItem)
        if (currentHistory.size > MAX_HISTORY_ITEMS) {
            currentHistory.subList(MAX_HISTORY_ITEMS, currentHistory.size).clear()
        }
        saveHistory(currentHistory)

        return newItem
    }

    fun getLastCopiedInfo(): Triple<String, Long, String>? {
        val text = prefs.getString(KEY_LAST_COPIED_TEXT, null) ?: return null
        val time = prefs.getLong(KEY_LAST_COPIED_TIME, 0L)
        val source = prefs.getString(KEY_LAST_COPY_SOURCE, "Copy") ?: "Copy"
        return Triple(text, time, source)
    }

    fun getHistory(): List<ClippingItem> {
        val jsonStr = prefs.getString(KEY_HISTORY_JSON, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<ClippingItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    ClippingItem(
                        id = obj.optString("id", i.toString()),
                        text = obj.optString("text", ""),
                        timestamp = obj.optLong("timestamp", 0L),
                        source = obj.optString("source", "Copy")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveHistory(items: List<ClippingItem>) {
        val jsonArray = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("text", item.text)
                put("timestamp", item.timestamp)
                put("source", item.source)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY_JSON, jsonArray.toString()).apply()
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY_JSON).apply()
    }
}
