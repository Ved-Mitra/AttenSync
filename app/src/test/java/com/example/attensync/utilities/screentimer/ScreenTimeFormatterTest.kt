package com.example.attensync.utilities.screentimer

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class ScreenTimeFormatterTest {
    @Test
    fun formatDuration_zeroMillis() {
        assertEquals("0m", ScreenTimeFormatter.formatDuration(0L))
    }

    @Test
    fun formatDuration_minutesOnly() {
        val millis = TimeUnit.MINUTES.toMillis(42)
        assertEquals("42m", ScreenTimeFormatter.formatDuration(millis))
    }

    @Test
    fun formatDuration_hoursAndMinutes() {
        val millis = TimeUnit.HOURS.toMillis(2) + TimeUnit.MINUTES.toMillis(5)
        assertEquals("2h 5m", ScreenTimeFormatter.formatDuration(millis))
    }
}

