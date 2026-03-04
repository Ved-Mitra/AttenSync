package com.example.attensync.data.leaderboard.model

import java.time.LocalDateTime

data class Assignment(
    val id: String,
    val title: String,
    val description: String?,
    val dueDate: LocalDateTime?,
    val courseName: String?
)