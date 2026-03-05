package com.example.attensync.utilities // Update to match your package!

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.attensync.R
import com.example.attensync.utilities.reminder.ReminderNotification
import com.example.attensync.utilities.reminder.ReminderNotificationStore
import com.example.attensync.utilities.reminder.ReminderStore
import kotlinx.coroutines.*

class FocusTrackingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var isTracking = false

    private val appUsageSeconds = mutableMapOf<String, Int>()
    private val pollIntervalSeconds = 5

    override fun onCreate() {
        super.onCreate()
        createForegroundChannel()
        createAlertChannel() // High priority channel for the actual warnings
        startForeground(1, createForegroundNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isTracking) {
            isTracking = true
            startTrackingLoop()
        }
        return START_STICKY
    }

    private fun startTrackingLoop() {
        serviceScope.launch {
            while (isActive) {
                val appIntervals = ReminderStore.loadAppIntervals(this@FocusTrackingService)
                val monitoredPackages = appIntervals.keys

                val foregroundApp = getForegroundPackageName()
                if (foregroundApp != null && monitoredPackages.contains(foregroundApp)) {
                    val intervalMinutes = appIntervals[foregroundApp] ?: 0
                    val intervalSeconds = intervalMinutes * 60
                    if (intervalSeconds > 0) {
                        val updatedSeconds =
                            (appUsageSeconds[foregroundApp] ?: 0) + pollIntervalSeconds
                        if (updatedSeconds >= intervalSeconds) {
                            Log.d("FocusTracker", "Reminder interval hit, pushing notification.")
                            pushWarningNotification(foregroundApp)
                            appUsageSeconds[foregroundApp] = updatedSeconds - intervalSeconds
                        } else {
                            appUsageSeconds[foregroundApp] = updatedSeconds
                        }
                    }
                }

                appUsageSeconds.keys
                    .filter { it !in monitoredPackages }
                    .forEach { appUsageSeconds.remove(it) }

                delay(pollIntervalSeconds * 1000L)
            }
        }
    }

    private fun pushWarningNotification(packageName: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val appName = try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            "this app"
        }

        val message = getString(R.string.reminder_notification_text)
        val notification = NotificationCompat.Builder(this, "AlertChannel")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(getString(R.string.reminder_notification_title))
            .setContentText(message)
            .setSubText(appName)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)

        ReminderNotificationStore.addNotification(
            this,
            ReminderNotification(appName, message, System.currentTimeMillis())
        )
    }

    private fun getForegroundPackageName(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 60

        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var currentForegroundApp: String? = null
        var currentForegroundTime = 0L

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            val isForegroundEvent = event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            if (isForegroundEvent && event.timeStamp > currentForegroundTime) {
                currentForegroundTime = event.timeStamp
                currentForegroundApp = event.packageName
            }
        }

        if (currentForegroundApp != null) {
            return currentForegroundApp
        }

        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            endTime - 1000 * 60 * 5,
            endTime
        )
        val mostRecent = usageStats
            .filter { it.lastTimeUsed > 0 }
            .maxByOrNull(UsageStats::getLastTimeUsed)

        return mostRecent?.packageName
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isTracking = false
        serviceScope.cancel()
    }

    // --- Channel Setups ---
    private fun createForegroundChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("TrackingChannel", "Active Tracking", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createAlertChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // IMPORTANCE_HIGH is required for "Heads Up" pop-down notifications
            val channel = NotificationChannel("AlertChannel", "Time Limit Alerts", NotificationManager.IMPORTANCE_HIGH)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, "TrackingChannel")
            .setContentTitle("AttentionOS is Active")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()
    }
}