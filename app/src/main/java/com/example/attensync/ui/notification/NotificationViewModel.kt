package com.example.attensync.ui.notification

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class NotificationViewModel : ViewModel() {
    private val _text = MutableLiveData<String>().apply {
        value = "Notification tab"
    }
    val text: LiveData<String> = _text
}