package com.example.attensync.utilities.screentimer

import java.util.concurrent.TimeUnit

object ScreenTimeFormatter {
    fun formatDuration(millis: Long): String {
        if (millis <= 0L) {
            return "0m"
        }
        val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }
}

