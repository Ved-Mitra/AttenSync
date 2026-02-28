package com.example.attensync.utilities.screentimer

data class ScreenTimeSummary(
    val totalMillis: Long,
    val entries: List<ScreenTimeEntry>,
    val hasUsageAccess: Boolean
)

