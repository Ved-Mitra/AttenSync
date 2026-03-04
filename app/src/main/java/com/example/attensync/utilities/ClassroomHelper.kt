package com.example.attensync.utilities

import com.example.attensync.data.model.Assignment
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.classroom.Classroom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class ClassroomHelper(credential: GoogleAccountCredential) {
    private val service: Classroom = Classroom.Builder(
        NetHttpTransport(),
        GsonFactory.getDefaultInstance(),
        credential
    ).setApplicationName("AttenSync").build()

    suspend fun fetchAssignments(): List<Assignment> = withContext(Dispatchers.IO) {
        val courses = service.courses().list().execute().courses ?: emptyList()
        courses.flatMap { course ->
            val courseWork = service.courses().courseWork().list(course.id).execute().courseWork ?: emptyList()
            courseWork.map { work ->
                val due = work.dueDate
                val time = work.dueTime
                val ldt = if (due != null) {
                    LocalDateTime.of(
                        due.year, due.month, due.day,
                        time?.hours ?: 0, time?.minutes ?: 0
                    )
                } else null
                
                Assignment(
                    id = work.id,
                    title = work.title ?: "Untitled",
                    description = work.description,
                    dueDate = ldt,
                    courseName = course.name
                )
            }
        }
    }
}
