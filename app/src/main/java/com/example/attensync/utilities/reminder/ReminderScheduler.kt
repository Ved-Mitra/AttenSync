package com.example.attensync.utilities.reminder

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.attensync.utilities.FocusTrackingService

object ReminderScheduler {
    fun applySettings(context: Context, intervalMinutes: Int, monitoredPackages: Set<String>) {
        ReminderStore.saveIntervalMinutes(context, intervalMinutes)
        ReminderStore.saveMonitoredPackages(context, monitoredPackages)

        if (monitoredPackages.isEmpty() || intervalMinutes <= 0) {
            stopTracking(context)
        } else {
            startTracking(context)
        }
    }

    fun startTracking(context: Context) {
        val intent = Intent(context, FocusTrackingService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopTracking(context: Context) {
        val intent = Intent(context, FocusTrackingService::class.java)
        context.stopService(intent)
    }
}

