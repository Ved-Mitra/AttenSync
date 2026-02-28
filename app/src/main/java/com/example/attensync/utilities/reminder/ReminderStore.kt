package com.example.attensync.utilities.reminder

import android.content.Context

object ReminderStore {
    private const val PREFS_NAME = "reminder_prefs"
    private const val KEY_INTERVAL_MINUTES = "interval_minutes"
    private const val KEY_MONITORED_PACKAGES = "monitored_packages"

    fun loadIntervalMinutes(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_INTERVAL_MINUTES, 30)
    }

    fun saveIntervalMinutes(context: Context, minutes: Int) {
        val safeMinutes = minutes.coerceAtLeast(1)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_INTERVAL_MINUTES, safeMinutes).apply()
    }

    fun loadMonitoredPackages(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getStringSet(KEY_MONITORED_PACKAGES, emptySet()) ?: emptySet()
        return raw.toSet()
    }

    fun saveMonitoredPackages(context: Context, packages: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_MONITORED_PACKAGES, packages).apply()
    }
}

