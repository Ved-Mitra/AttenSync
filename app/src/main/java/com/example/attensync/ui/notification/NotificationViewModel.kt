package com.example.attensync.ui.notification

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.attensync.utilities.reminder.ReminderNotification
import com.example.attensync.utilities.reminder.ReminderNotificationStore

class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val _notifications = MutableLiveData<List<ReminderNotification>>()
    val notifications: LiveData<List<ReminderNotification>> = _notifications

    fun refresh() {
        _notifications.value = ReminderNotificationStore.loadNotifications(getApplication())
    }
}