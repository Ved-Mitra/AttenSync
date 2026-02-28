package com.example.attensync.utilities.reminder

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ReminderNotificationStore {
    private const val PREFS_NAME = "reminder_prefs"
    private const val KEY_NOTIFICATION_HISTORY = "notification_history"
    private const val MAX_ITEMS = 10

    fun addNotification(context: Context, notification: ReminderNotification) {
        val existing = loadNotifications(context).toMutableList()
        existing.add(0, notification)
        val trimmed = existing.take(MAX_ITEMS)
        saveNotifications(context, trimmed)
    }

    fun loadNotifications(context: Context): List<ReminderNotification> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_NOTIFICATION_HISTORY, null) ?: return emptyList()
        val array = try {
            JSONArray(raw)
        } catch (e: Exception) {
            return emptyList()
        }
        val result = mutableListOf<ReminderNotification>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val appName = item.optString("appName", "")
            val message = item.optString("message", "")
            val timestamp = item.optLong("timestamp", 0L)
            if (appName.isNotBlank() && message.isNotBlank() && timestamp > 0L) {
                result.add(ReminderNotification(appName, message, timestamp))
            }
        }
        return result
    }

    private fun saveNotifications(context: Context, notifications: List<ReminderNotification>) {
        val array = JSONArray()
        notifications.forEach { notification ->
            val obj = JSONObject()
            obj.put("appName", notification.appName)
            obj.put("message", notification.message)
            obj.put("timestamp", notification.timestampMillis)
            array.put(obj)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_NOTIFICATION_HISTORY, array.toString()).apply()
    }
}

