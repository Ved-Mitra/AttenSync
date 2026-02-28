package com.example.attensync.data.leaderboard

import android.content.Context

class LeaderboardRepository(
    private val context: Context,
    private val api: LeaderboardApi = LeaderboardService.createApi()
) {
    suspend fun refreshLeaderboard(limit: Int = 20): Result<LeaderboardResponse> {
        return runCatching {
            val response = api.fetchLeaderboard(limit)
            LeaderboardStore.saveEntries(context, response.entries, response.updatedAt)
            response
        }
    }

    suspend fun refreshPoints(userId: String): Result<PointsResponse> {
        return runCatching {
            val response = api.fetchPoints(userId)
            LeaderboardStore.savePoints(context, response.points)
            response
        }
    }

    suspend fun sendDailyUsage(request: UsageUploadRequest): Result<UsageUploadResponse> {
        return runCatching {
            api.sendUsage(request)
        }
    }

    fun loadCachedLeaderboard(): Pair<List<LeaderboardEntry>, String?> {
        return LeaderboardStore.loadEntries(context)
    }

    fun loadCachedPoints(): Double {
        return LeaderboardStore.loadPoints(context)
    }
}
