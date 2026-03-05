package com.example.attensync.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.attensync.data.leaderboard.LeaderboardStore
import com.example.attensync.utilities.reminder.MonitoredApp
import com.example.attensync.utilities.reminder.ReminderAppProvider
import com.example.attensync.utilities.reminder.ReminderStore
import com.example.attensync.utilities.screentimer.ScreenTimeFormatter
import com.example.attensync.utilities.screentimer.ScreenTimeRepository
import com.example.attensync.utilities.screentimer.ScreenTimeSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val _text = MutableLiveData<String>().apply {
        value = "Dashboard Tab"
    }
    val text: LiveData<String> = _text

    private val _points = MutableLiveData(0)
    val points: LiveData<Int> = _points

    private val _reminderIntervals = MutableLiveData<Map<String, Int>>()
    val reminderIntervals: LiveData<Map<String, Int>> = _reminderIntervals

    private val _monitoredApps = MutableLiveData<List<MonitoredApp>>()
    val monitoredApps: LiveData<List<MonitoredApp>> = _monitoredApps

    private val screenTimeRepository = ScreenTimeRepository()

    private val _screenTimeUiState = MutableLiveData<ScreenTimeUiState>()
    val screenTimeUiState: LiveData<ScreenTimeUiState> = _screenTimeUiState

    init {
        loadReminderConfig()
        refreshApps()
        refreshScreenTime()
        refreshPoints()
    }

    private fun loadReminderConfig() {
        val intervals = ReminderStore.loadAppIntervals(getApplication())
        _reminderIntervals.value = intervals
    }

    fun refreshApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = ReminderAppProvider.loadLaunchableApps(getApplication())
            withContext(Dispatchers.Main) {
                _monitoredApps.value = apps
            }
        }
    }

    fun refreshScreenTime() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val monitoredPackages = ReminderStore.loadMonitoredPackages(context)
            val summary = screenTimeRepository.loadTodayScreenTime(context, monitoredPackages)
            val uiState = buildScreenTimeUiState(summary, monitoredPackages.isNotEmpty())
            withContext(Dispatchers.Main) {
                _screenTimeUiState.value = uiState
            }
        }
    }

    private fun buildScreenTimeUiState(
        summary: ScreenTimeSummary,
        hasMonitoredApps: Boolean
    ): ScreenTimeUiState {
        val totalText = ScreenTimeFormatter.formatDuration(summary.totalMillis)
        val maxMillis = TimeUnit.HOURS.toMillis(24)
        val percent = if (summary.totalMillis <= 0L) {
            0
        } else {
            ((summary.totalMillis.toDouble() / maxMillis) * 100).roundToInt().coerceIn(0, 100)
        }
        val entries = summary.entries.map { entry ->
            ScreenTimeEntryUi(
                packageName = entry.packageName,
                label = entry.label,
                timeText = ScreenTimeFormatter.formatDuration(entry.totalMillis),
                totalMillis = entry.totalMillis
            )
        }
        return ScreenTimeUiState(
            totalMillis = summary.totalMillis,
            totalText = totalText,
            progressPercent = percent,
            entries = entries,
            hasUsageAccess = summary.hasUsageAccess,
            hasMonitoredApps = hasMonitoredApps
        )
    }

    fun saveReminderConfig(appIntervals: Map<String, Int>) {
        ReminderStore.saveAppIntervals(getApplication(), appIntervals)
        ReminderStore.saveMonitoredPackages(getApplication(), appIntervals.keys)
        _reminderIntervals.value = appIntervals
        _monitoredApps.value = _monitoredApps.value?.map { app ->
            app.copy(isMonitored = appIntervals.containsKey(app.packageName))
        }
        refreshScreenTime()
    }

    fun refreshPoints() {
        val points = LeaderboardStore.loadPoints(getApplication())
        _points.value = points.toInt()
    }

    fun updatePoints(points: Int) {
        _points.value = points
    }
}

data class ScreenTimeUiState(
    val totalMillis: Long,
    val totalText: String,
    val progressPercent: Int,
    val entries: List<ScreenTimeEntryUi>,
    val hasUsageAccess: Boolean,
    val hasMonitoredApps: Boolean
)

data class ScreenTimeEntryUi(
    val packageName: String,
    val label: String,
    val timeText: String,
    val totalMillis: Long
)
