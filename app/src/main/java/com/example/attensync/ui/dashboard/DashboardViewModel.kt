package com.example.attensync.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.attensync.utilities.reminder.MonitoredApp
import com.example.attensync.utilities.reminder.ReminderAppProvider
import com.example.attensync.utilities.reminder.ReminderStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val _text = MutableLiveData<String>().apply {
        value = "Dashboard Tab"
    }
    val text: LiveData<String> = _text

    private val _reminderIntervalMinutes = MutableLiveData<Int>()
    val reminderIntervalMinutes: LiveData<Int> = _reminderIntervalMinutes

    private val _monitoredApps = MutableLiveData<List<MonitoredApp>>()
    val monitoredApps: LiveData<List<MonitoredApp>> = _monitoredApps

    init {
        loadReminderConfig()
        refreshApps()
    }

    private fun loadReminderConfig() {
        val minutes = ReminderStore.loadIntervalMinutes(getApplication())
        _reminderIntervalMinutes.value = minutes
    }

    fun refreshApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = ReminderAppProvider.loadLaunchableApps(getApplication())
            withContext(Dispatchers.Main) {
                _monitoredApps.value = apps
            }
        }
    }

    fun saveReminderConfig(intervalMinutes: Int, monitoredPackages: Set<String>) {
        ReminderStore.saveIntervalMinutes(getApplication(), intervalMinutes)
        ReminderStore.saveMonitoredPackages(getApplication(), monitoredPackages)
        _reminderIntervalMinutes.value = intervalMinutes
        _monitoredApps.value = _monitoredApps.value?.map { app ->
            app.copy(isMonitored = monitoredPackages.contains(app.packageName))
        }
    }
}