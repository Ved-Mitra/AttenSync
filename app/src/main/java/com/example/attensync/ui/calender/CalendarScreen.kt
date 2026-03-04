package com.example.attensync.ui.calender

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*

@Composable
fun CalendarScreen(assignments: List<Assignment>) {
    val currentMonth = remember { YearMonth.now() }
    // Now showing only 12 months starting from current month (No past months)
    val months = remember { (0..11).map { currentMonth.plusMonths(it.toLong()) } }
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            SummaryHeader(assignments)
        }
        items(months) { month ->
            MonthItem(month, assignments)
        }
    }
}

@Composable
fun SummaryHeader(assignments: List<Assignment>) {
    val totalWithDueDate = assignments.count { it.dueDate != null }
    val totalWithoutDueDate = assignments.size - totalWithDueDate

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Assignment Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(text = "Total Assignments: ${assignments.size}")
            Text(text = "With Due Date: $totalWithDueDate")
            if (totalWithoutDueDate > 0) {
                Text(text = "Without Due Date: $totalWithoutDueDate", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun MonthItem(month: YearMonth, assignments: List<Assignment>) {
    Column {
        Text(
            text = month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + month.year,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val daysInMonth = month.lengthOfMonth()
        val firstDayOfMonth = month.atDay(1).dayOfWeek.value % 7 // 0 for Sunday

        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        val totalGridItems = ((daysInMonth + firstDayOfMonth + 6) / 7) * 7
        
        Column {
            for (row in 0 until (totalGridItems / 7)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val dayIndex = row * 7 + col - firstDayOfMonth + 1
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dayIndex in 1..daysInMonth) {
                                val date = month.atDay(dayIndex)
                                val dayAssignments = assignments.filter { it.dueDate == date }
                                
                                DayItem(dayIndex, dayAssignments, date == LocalDate.now())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayItem(day: Int, assignments: List<Assignment>, isToday: Boolean) {
    var showDialog by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isToday) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                shape = MaterialTheme.shapes.small
            )
            .clickable { if (assignments.isNotEmpty()) showDialog = true }
    ) {
        Text(
            text = day.toString(),
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        if (assignments.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color.Red, shape = MaterialTheme.shapes.extraSmall)
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Assignments for Day $day") },
            text = {
                LazyColumn {
                    items(assignments) { assignment ->
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(text = assignment.title, fontWeight = FontWeight.Bold)
                            Text(text = assignment.courseName, style = MaterialTheme.typography.bodySmall)
                            assignment.description?.let {
                                Text(text = it, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                            }
                            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
