package com.example.attensync.data.leaderboard

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object LeaderboardService {
    private const val BASE_URL = "https://attensync-backend.onrender.com/"

    fun createApi(): LeaderboardApi {
        val gson = GsonBuilder().create()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(LeaderboardApi::class.java)
    }
}
