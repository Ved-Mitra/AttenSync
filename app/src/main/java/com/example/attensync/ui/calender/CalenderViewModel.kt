package com.example.attensync.ui.calender

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CalenderViewModel : ViewModel() {
    // This holds your data. The fragment will listen to this.
    private val _text = MutableLiveData<String>().apply {
        value = "Calendar Data Loading..."
    }
    val text: LiveData<String> = _text
}