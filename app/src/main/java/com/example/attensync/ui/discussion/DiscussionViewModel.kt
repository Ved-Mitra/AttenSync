package com.example.attensync.ui.discussion

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DiscussionViewModel : ViewModel() {
    private val _text = MutableLiveData<String>().apply {
        value = "Discussion Tab"
    }
    val text: LiveData<String> = _text
}