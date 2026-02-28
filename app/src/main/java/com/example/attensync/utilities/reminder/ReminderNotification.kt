package com.example.attensync.utilities.reminder

data class ReminderNotification(
    val appName: String,
    val message: String,
    val timestampMillis: Long
)

