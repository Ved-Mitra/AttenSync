package com.example.attensync.ui.leaderboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.attensync.data.leaderboard.LeaderboardEntry
import com.example.attensync.data.leaderboard.LeaderboardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class LeaderboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LeaderboardRepository(application.applicationContext)

    private val _uiState = MutableLiveData(LeaderboardUiState())
    val uiState: LiveData<LeaderboardUiState> = _uiState

    init {
        loadCached()
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val cached = repository.loadCachedLeaderboard()
            val cachedPoints = repository.loadCachedPoints()
            val result = repository.refreshLeaderboard()
            val pointsResult = currentUserId()?.let { repository.refreshPoints(it) }
            val updatedPoints = pointsResult?.getOrNull()?.points ?: cachedPoints
            val updated = result.getOrNull()
            val state = if (updated != null) {
                LeaderboardUiState(
                    rows = buildRows(updated.entries),
                    points = updatedPoints,
                    updatedAt = updated.updatedAt,
                    isOffline = false
                )
            } else {
                LeaderboardUiState(
                    rows = buildRows(cached.first),
                    points = cachedPoints,
                    updatedAt = cached.second,
                    isOffline = true
                )
            }
            withContext(Dispatchers.Main) {
                _uiState.value = state
            }
        }
    }

    private fun loadCached() {
        val cached = repository.loadCachedLeaderboard()
        val cachedPoints = repository.loadCachedPoints()
        _uiState.value = LeaderboardUiState(
            rows = buildRows(cached.first),
            points = cachedPoints,
            updatedAt = cached.second,
            isOffline = true
        )
    }

    private fun buildRows(entries: List<LeaderboardEntry>): List<LeaderboardRow> {
        if (entries.isEmpty()) return emptyList()
        val rows = mutableListOf<LeaderboardRow>()
        var currentRank = 1
        var lastPoints = entries.first().points
        entries.forEachIndexed { index, entry ->
            if (!pointsEqual(entry.points, lastPoints)) {
                currentRank = index + 1
                lastPoints = entry.points
            }
            rows.add(LeaderboardRow(rank = currentRank, entry = entry))
        }
        return rows
    }

    private fun pointsEqual(a: Double, b: Double): Boolean {
        return abs(a - b) < 0.0001
    }

    private fun currentUserId(): String? {
        return com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    }
}

data class LeaderboardUiState(
    val rows: List<LeaderboardRow> = emptyList(),
    val points: Double = 0.0,
    val updatedAt: String? = null,
    val isOffline: Boolean = true
)

data class LeaderboardRow(
    val rank: Int,
    val entry: LeaderboardEntry
)
