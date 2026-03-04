package com.example.attensync.ui.calender

import java.time.LocalDate

data class Assignment(
    val title: String,
    val courseName: String,
    val dueDate: LocalDate?,
    val description: String? = null
)