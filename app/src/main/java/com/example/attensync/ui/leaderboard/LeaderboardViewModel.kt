package com.example.attensync.ui.leaderboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LeaderboardViewModel : ViewModel() {
    private val _text = MutableLiveData<String>().apply {
        value = "Leaderboard tab"
    }
    val text: LiveData<String> = _text
}