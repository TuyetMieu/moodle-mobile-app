package com.example.test.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.test.data.model.ClassSession
import com.example.test.data.model.LearningData
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TimetableScreen(
    data: LearningData?,
    loading: Boolean,
    onShowClasswork: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedDay by rememberSaveable { mutableLongStateOf(startOfDay(System.currentTimeMillis())) }
    var selectedCourseId by rememberSaveable { mutableStateOf<String?>(null) }
    val days = weekDays(selectedDay)
    val weekday = calendarWeekday(selectedDay)
    val sessions = data?.sessions.orEmpty()
        .filter { it.weekday == weekday }
        .filter { session -> isInSemester(selectedDay, data) }
        .sortedBy { it.start }

    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Timetable", style = MaterialTheme.typography.headlineSmall)
                    Text("Week of ${dateLabel(days.first())}")
                }
                Button(onClick = onLogout) { Text("Sign out") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { selectedDay = addDays(selectedDay, -7) }) { Text("Previous week") }
                Button(onClick = { selectedDay = startOfDay(System.currentTimeMillis()) }) { Text("Today") }
                Button(onClick = { selectedDay = addDays(selectedDay, 7) }) { Text("Next week") }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEach { day ->
                    FilterChip(
                        selected = startOfDay(day) == startOfDay(selectedDay),
                        onClick = { selectedDay = day },
                        label = {
                            Text(
                                SimpleDateFormat("EEE d", Locale.getDefault()).format(Date(day))
                            )
                        }
                    )
                }
            }
            Text(
                SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date(selectedDay)),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            if (loading && data == null) {
                Text("Loading timetable…")
            } else if (sessions.isEmpty()) {
                Text("No classes scheduled for this day.")
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sessions, key = { it.id }) { session ->
                        SessionCard(
                            session = session,
                            courseTitle = data?.courses?.firstOrNull { it.id == session.courseId }?.title
                                ?: session.courseId,
                            selected = selectedCourseId == session.courseId,
                            onClick = {
                                selectedCourseId =
                                    if (selectedCourseId == session.courseId) null else session.courseId
                            }
                        )
                    }
                }
            }
            data?.courses?.firstOrNull { it.id == selectedCourseId }?.let { course ->
                Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(course.title, style = MaterialTheme.typography.titleMedium)
                        Text("${course.code} · ${course.credits} credits")
                        Text("${course.done}/${course.total} assignments complete")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onShowClasswork, modifier = Modifier.fillMaxWidth()) {
                Text("Back to classwork")
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: ClassSession,
    courseTitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("$courseTitle · ${session.kind}", style = MaterialTheme.typography.titleMedium)
            Text("${session.start}–${session.end} · ${session.room}")
            if (selected) Text("Course selected")
        }
    }
}

private fun weekDays(date: Long): List<Long> {
    val calendar = Calendar.getInstance().apply { timeInMillis = startOfDay(date) }
    val daysSinceMonday = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
    calendar.add(Calendar.DAY_OF_MONTH, -daysSinceMonday)
    return List(7) { index ->
        Calendar.getInstance().apply {
            timeInMillis = calendar.timeInMillis
            add(Calendar.DAY_OF_MONTH, index)
        }.timeInMillis
    }
}

private fun startOfDay(time: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = time
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

private fun addDays(time: Long, amount: Int): Long =
    Calendar.getInstance().apply {
        timeInMillis = time
        add(Calendar.DAY_OF_MONTH, amount)
    }.timeInMillis

private fun calendarWeekday(time: Long): Int =
    Calendar.getInstance().apply { timeInMillis = time }.get(Calendar.DAY_OF_WEEK) - 1

private fun isInSemester(date: Long, data: LearningData?): Boolean {
    if (data == null || data.semesterStartsAt == 0L || data.semesterEndsAt == 0L) return true
    return date >= startOfDay(data.semesterStartsAt) &&
        date <= startOfDay(data.semesterEndsAt)
}

private fun dateLabel(time: Long): String =
    SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(time))
