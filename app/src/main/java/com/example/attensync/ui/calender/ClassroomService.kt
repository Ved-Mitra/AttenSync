package com.example.attensync.ui.calender

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.classroom.Classroom
import com.google.api.services.classroom.ClassroomScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class ClassroomService(private val context: Context) {

    private fun getService(): Classroom? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(
                ClassroomScopes.CLASSROOM_COURSES_READONLY,
                ClassroomScopes.CLASSROOM_COURSEWORK_ME_READONLY
            )
        ).apply {
            selectedAccount = account.account
        }

        return Classroom.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("AttenSync").build()
    }

    suspend fun fetchAssignments(): List<Assignment> = withContext(Dispatchers.IO) {
        val service = getService() ?: run {
            Log.e("ClassroomService", "No signed-in account found")
            return@withContext emptyList<Assignment>()
        }
        val assignments = mutableListOf<Assignment>()

        try {
            // Explicitly list ACTIVE courses
            val courses = service.courses().list()
                .setCourseStates(listOf("ACTIVE"))
                .execute()
                .courses ?: emptyList()

            Log.d("ClassroomService", "Found ${courses.size} active courses")

            for (course in courses) {
                // Fetch coursework (assignments) for each course
                val courseWork = service.courses().courseWork().list(course.id)
                    .execute()
                    .courseWork ?: emptyList()

                Log.d("ClassroomService", "Course: ${course.name}, Assignments: ${courseWork.size}")

                for (work in courseWork) {
                    val dueDate = work.dueDate
                    val localDate =
                        if (dueDate != null && dueDate.year != null && dueDate.month != null && dueDate.day != null) {
                            try {
                                LocalDate.of(dueDate.year, dueDate.month, dueDate.day)
                            } catch (e: Exception) {
                                null
                            }
                        } else {
                            null
                        }

                    assignments.add(
                        Assignment(
                            title = work.title ?: "Untitled",
                            courseName = course.name ?: "Unknown",
                            dueDate = localDate,
                            description = work.description
                        )
                    )
                }
            }
        } catch (e: UserRecoverableAuthIOException) {
            // This happens if the user needs to grant permissions again
            Log.e("ClassroomService", "User authorization required: ${e.message}")
        } catch (e: Exception) {
            Log.e("ClassroomService", "Error fetching from Classroom API", e)
            e.printStackTrace()
        }

        return@withContext assignments.sortedBy { it.dueDate }
    }
}