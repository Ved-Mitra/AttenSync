package com.example.attensync.ui.redeem

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RedeemViewModel : ViewModel() {
    private val _text = MutableLiveData<String>().apply {
        value = "Weekly Focus Report"
    }
    val text: LiveData<String> = _text
}