package com.example.attensync.data.leaderboard

data class LeaderboardEntry(
    val userId: String,
    val userName: String,
    val points: Double
)

data class LeaderboardResponse(
    val updatedAt: String,
    val entries: List<LeaderboardEntry>
)

data class PointsResponse(
    val userId: String,
    val points: Double
)

data class UsageUploadRequest(
    val userId: String,
    val userName: String,
    val date: String,
    val apps: List<UsageAppEntry>
)

data class UsageAppEntry(
    val packageName: String,
    val minutes: Int,
    val factor: Double = 1.0
)

data class UsageUploadResponse(
    val userId: String,
    val date: String,
    val delta: Double,
    val total: Double
)
