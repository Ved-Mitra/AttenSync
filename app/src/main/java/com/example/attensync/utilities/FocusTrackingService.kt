package com.example.attensync.utilities // Update to match your package!

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class FocusTrackingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var isTracking = false

    // MVP Mock Database: Package Name -> Threshold limit in seconds
    // In a full app, you would fetch these numbers from SharedPreferences or Room DB
    private val appThresholds = mapOf(
        "com.instagram.android" to 15, // 15 seconds for testing
        "com.zhiliaoapp.musically" to 20, // TikTok
        "com.google.android.youtube" to 30
    )

    private var currentSessionSeconds = 0

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
                val foregroundApp = getForegroundPackageName()

                if (foregroundApp != null && appThresholds.containsKey(foregroundApp)) {
                    currentSessionSeconds += 5 // Checking every 5 seconds
                    val limit = appThresholds[foregroundApp] ?: 9999

                    if (currentSessionSeconds >= limit) {
                        Log.d("FocusTracker", "Threshold hit! Pushing notification.")
                        pushWarningNotification(foregroundApp)

                        // Reset so we don't spam them with 100 notifications a minute
                        currentSessionSeconds = -60
                    }
                } else {
                    currentSessionSeconds = 0 // Reset if they leave the app
                }

                delay(5000)
            }
        }
    }

    private fun pushWarningNotification(packageName: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Make the package name look pretty for the notification
        val appName = when(packageName) {
            "com.instagram.android" -> "Instagram"
            "com.zhiliaoapp.musically" -> "TikTok"
            "com.google.android.youtube" -> "YouTube"
            else -> "this app"
        }

        val notification = NotificationCompat.Builder(this, "AlertChannel")
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // Built-in Android warning icon
            .setContentTitle("AttentionOS Alert ⏳")
            .setContentText("You've exceeded your daily limit for $appName.")
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Forces it to drop down from the top of the screen
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Sound and vibration
            .setAutoCancel(true)
            .build()

        // Use a random ID so multiple notifications can stack if needed
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun getForegroundPackageName(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 10

        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var currentForegroundApp: String? = null

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                currentForegroundApp = event.packageName
            }
        }
        return currentForegroundApp
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