package com.example.attensync.data.model

import java.time.LocalDateTime

data class Assignment(
    val id: String,
    val title: String,
    val description: String?,
    val dueDate: LocalDateTime?,
    val courseName: String?
)
