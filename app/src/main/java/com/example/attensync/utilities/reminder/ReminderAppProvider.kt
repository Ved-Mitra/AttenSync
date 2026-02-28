package com.example.attensync.utilities.reminder

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

object ReminderAppProvider {
    fun loadLaunchableApps(context: Context): List<MonitoredApp> {
        val pm = context.packageManager
        val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val queryFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PackageManager.MATCH_ALL
        } else {
            @Suppress("DEPRECATION")
            PackageManager.MATCH_DEFAULT_ONLY
        }
        val activities = pm.queryIntentActivities(launchIntent, queryFlags)
        val monitored = ReminderStore.loadMonitoredPackages(context)
        val packages = activities.map { it.activityInfo.packageName }.distinct()

        return packages
            .filter { it != context.packageName }
            .mapNotNull { packageName ->
                val label = try {
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    null
                }
                label?.let {
                    MonitoredApp(packageName, it, monitored.contains(packageName))
                }
            }
            .sortedBy { it.label.lowercase() }
    }
}
