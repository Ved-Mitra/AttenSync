package com.example.attensync.utilities.screentimer

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import java.util.Calendar
import kotlin.math.max

class ScreenTimeRepository {
    fun loadTodayScreenTime(context: Context, monitoredPackages: Set<String>): ScreenTimeSummary {
        val hasAccess = hasUsageAccess(context)
        if (!hasAccess) {
            return ScreenTimeSummary(0L, emptyList(), false)
        }
        if (monitoredPackages.isEmpty()) {
            return ScreenTimeSummary(0L, emptyList(), true)
        }

        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = startOfDayMillis()
        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: emptyList()
        val usageByPackage = usageStats.associateBy { it.packageName }
        val pm = context.packageManager

        val entries = monitoredPackages.mapNotNull { packageName ->
            val label = try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                null
            }
            val usageMillis = usageByPackage[packageName]?.let { usage ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    max(usage.totalTimeVisible, usage.totalTimeInForeground)
                } else {
                    usage.totalTimeInForeground
                }
            } ?: 0L
            label?.let {
                ScreenTimeEntry(packageName, it, usageMillis)
            }
        }.sortedByDescending { it.totalMillis }

        val totalMillis = entries.sumOf { it.totalMillis }
        return ScreenTimeSummary(totalMillis, entries, true)
    }

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun startOfDayMillis(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}
