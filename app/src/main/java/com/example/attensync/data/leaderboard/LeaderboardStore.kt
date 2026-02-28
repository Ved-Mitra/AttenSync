package com.example.attensync.data.leaderboard

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object LeaderboardStore {
    private const val PREFS_NAME = "leaderboard_prefs"
    private const val KEY_ENTRIES = "entries_json"
    private const val KEY_POINTS = "points_value"
    private const val KEY_UPDATED_AT = "updated_at"

    fun saveEntries(context: Context, entries: List<LeaderboardEntry>, updatedAt: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(entries)
        prefs.edit()
            .putString(KEY_ENTRIES, json)
            .putString(KEY_UPDATED_AT, updatedAt)
            .apply()
    }

    fun loadEntries(context: Context): Pair<List<LeaderboardEntry>, String?> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_ENTRIES, null) ?: return emptyList<LeaderboardEntry>() to null
        val type = object : TypeToken<List<LeaderboardEntry>>() {}.type
        val entries = Gson().fromJson<List<LeaderboardEntry>>(json, type) ?: emptyList()
        val updatedAt = prefs.getString(KEY_UPDATED_AT, null)
        return entries to updatedAt
    }

    fun savePoints(context: Context, points: Double) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat(KEY_POINTS, points.toFloat()).apply()
    }

    fun loadPoints(context: Context): Double {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(KEY_POINTS, 0f).toDouble()
    }
}

