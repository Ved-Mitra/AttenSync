package com.example.attensync.data.leaderboard

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface LeaderboardApi {
    @GET("/v1/leaderboard")
    suspend fun fetchLeaderboard(@Query("limit") limit: Int = 20): LeaderboardResponse

    @GET("/v1/points/{userId}")
    suspend fun fetchPoints(@Path("userId") userId: String): PointsResponse

    @POST("/v1/usage")
    suspend fun sendUsage(@Body request: UsageUploadRequest): UsageUploadResponse
}
