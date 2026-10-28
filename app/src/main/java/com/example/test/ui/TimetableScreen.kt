package com.example.test.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.test.data.model.ClassSession
import com.example.test.data.model.LearningData
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

private enum class CalendarView {
    WEEK,
    MONTH
}

private enum class SessionStatus(val label: String) {
    IN_PROGRESS("In progress"),
    UPCOMING("Upcoming"),
    ENDED("Ended")
}

@Composable
fun TimetableScreen(
    data: LearningData?,
    loading: Boolean,
    offline: Boolean,
    error: String?,
    snackbarHostState: SnackbarHostState,
    onRetry: () -> Unit,
    onShowClasswork: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedDay by rememberSaveable { mutableLongStateOf(startOfDay(System.currentTimeMillis())) }
    var selectedCourseId by rememberSaveable { mutableStateOf<String?>(null) }
    var calendarView by rememberSaveable { mutableStateOf(CalendarView.WEEK) }
    var now by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            now = System.currentTimeMillis()
        }
    }
    val days = if (calendarView == CalendarView.MONTH) monthDays(selectedDay) else weekDays(selectedDay)
    val today = startOfDay(System.currentTimeMillis())
    val weekday = calendarWeekday(selectedDay)
    val sessions = data?.sessions.orEmpty()
        .filter { it.weekday == weekday }
        .filter { session -> isInSemester(selectedDay, data) }
        .sortedBy { it.start }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Timetable", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        if (calendarView == CalendarView.MONTH) monthLabel(selectedDay)
                        else "Week of ${dateLabel(days.first())}"
                    )
                }
                Button(onClick = onLogout) { Text("Sign out") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    selectedDay = if (calendarView == CalendarView.MONTH) addMonths(selectedDay, -1)
                    else addDays(selectedDay, -7)
                }) { Text("Previous") }
                Button(onClick = { selectedDay = today }) { Text("Today") }
                Button(onClick = {
                    selectedDay = if (calendarView == CalendarView.MONTH) addMonths(selectedDay, 1)
                    else addDays(selectedDay, 7)
                }) { Text("Next") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalendarView.entries.forEach { view ->
                    FilterChip(
                        selected = calendarView == view,
                        onClick = { calendarView = view },
                        label = { Text(if (view == CalendarView.WEEK) "Week" else "Month") }
                    )
                }
            }
            if (calendarView == CalendarView.WEEK) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    days.forEach { day ->
                        FilterChip(
                            selected = startOfDay(day) == startOfDay(selectedDay),
                            onClick = { selectedDay = day },
                            label = {
                                Text(SimpleDateFormat("EEE d", Locale.getDefault()).format(Date(day)))
                            }
                        )
                    }
                }
            } else {
                MonthGrid(
                    dates = days,
                    selectedDay = selectedDay,
                    today = today,
                    displayedMonth = yearMonth(selectedDay),
                    sessions = data?.sessions.orEmpty(),
                    semesterData = data,
                    onSelect = { selectedDay = it }
                )
            }
            Text(
                SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date(selectedDay)),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            if (loading && data == null) {
                Text("Loading timetable…")
            } else if (data == null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(error ?: "Timetable could not be loaded.", color = MaterialTheme.colorScheme.error)
                    Button(onClick = onRetry, enabled = !loading) { Text("Retry") }
                }
            } else if (offline) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Offline · showing saved data",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = onRetry, enabled = !loading) { Text("Retry") }
                }
            } else if (error != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                    Button(onClick = onRetry, enabled = !loading) { Text("Retry") }
                }
            }

            if (data != null && sessions.isEmpty()) {
                Text(
                    if (isInSemester(selectedDay, data)) "No classes scheduled for this day."
                    else "This date is outside the semester."
                )
            } else if (data != null) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sessions, key = { it.id }) { session ->
                        SessionCard(
                            session = session,
                            courseTitle = data.courses.firstOrNull { it.id == session.courseId }?.title
                                ?: session.courseId,
                            courseTone = data.courses.firstOrNull { it.id == session.courseId }?.tone.orEmpty(),
                            status = sessionStatus(selectedDay, session, now),
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
    courseTone: String,
    status: SessionStatus,
    selected: Boolean,
    onClick: () -> Unit
) {
    val tone = courseColor(courseTone)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, tone.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (selected) tone.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("$courseTitle · ${session.kind}", style = MaterialTheme.typography.titleMedium)
            Text("${session.start}–${session.end} · ${session.room}")
            Text(status.label, color = tone)
            if (selected) Text("Course selected")
        }
    }
}

@Composable
private fun MonthGrid(
    dates: List<Long>,
    selectedDay: Long,
    today: Long,
    displayedMonth: Int,
    sessions: List<ClassSession>,
    semesterData: LearningData?,
    onSelect: (Long) -> Unit
) {
    val weekdayHeadings = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    Row(Modifier.fillMaxWidth()) {
        weekdayHeadings.forEach { heading ->
            Text(heading, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
        }
    }
    dates.chunked(7).forEach { week ->
        Row(Modifier.fillMaxWidth()) {
            week.forEach { date ->
                val isToday = startOfDay(date) == today
                val isSelected = startOfDay(date) == startOfDay(selectedDay)
                val hasSessions = isInSemester(date, semesterData) &&
                    sessions.any { it.weekday == calendarWeekday(date) }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent
                        )
                        .then(
                            if (isToday) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            else Modifier
                        )
                        .clickable { onSelect(date) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            Calendar.getInstance().apply { timeInMillis = date }
                                .get(Calendar.DAY_OF_MONTH).toString(),
                            color = if (yearMonth(date) == displayedMonth) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (hasSessions) {
                            Box(
                                Modifier
                                    .padding(top = 2.dp)
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
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

private fun monthDays(date: Long): List<Long> {
    val first = Calendar.getInstance().apply {
        timeInMillis = startOfDay(date)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val daysBefore = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val firstCell = Calendar.getInstance().apply {
        timeInMillis = first.timeInMillis
        add(Calendar.DAY_OF_MONTH, -daysBefore)
    }
    val cellCount = ((daysBefore + first.getActualMaximum(Calendar.DAY_OF_MONTH) + 6) / 7) * 7
    return List(cellCount) { index ->
        Calendar.getInstance().apply {
            timeInMillis = firstCell.timeInMillis
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

private fun addMonths(time: Long, amount: Int): Long =
    Calendar.getInstance().apply {
        timeInMillis = time
        val day = get(Calendar.DAY_OF_MONTH)
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, amount)
        val lastDay = getActualMaximum(Calendar.DAY_OF_MONTH)
        set(Calendar.DAY_OF_MONTH, minOf(day, lastDay))
    }.timeInMillis

private fun calendarWeekday(time: Long): Int =
    Calendar.getInstance().apply { timeInMillis = time }.get(Calendar.DAY_OF_WEEK) - 1

private fun yearMonth(time: Long): Int =
    Calendar.getInstance().apply { timeInMillis = time }.let {
        it.get(Calendar.YEAR) * 12 + it.get(Calendar.MONTH)
    }

private fun isInSemester(date: Long, data: LearningData?): Boolean {
    if (data == null || data.semesterStartsAt == 0L || data.semesterEndsAt == 0L) return true
    return date >= startOfDay(data.semesterStartsAt) &&
        date <= startOfDay(data.semesterEndsAt)
}

private fun dateLabel(time: Long): String =
    SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(time))

private fun monthLabel(time: Long): String =
    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(time))

private fun sessionStatus(day: Long, session: ClassSession, now: Long): SessionStatus {
    val calendar = Calendar.getInstance().apply { timeInMillis = startOfDay(day) }
    val startParts = session.start.split(":")
    val endParts = session.end.split(":")
    val start = Calendar.getInstance().apply {
        timeInMillis = calendar.timeInMillis
        set(Calendar.HOUR_OF_DAY, startParts[0].toInt())
        set(Calendar.MINUTE, startParts[1].toInt())
    }.timeInMillis
    val end = Calendar.getInstance().apply {
        timeInMillis = calendar.timeInMillis
        set(Calendar.HOUR_OF_DAY, endParts[0].toInt())
        set(Calendar.MINUTE, endParts[1].toInt())
    }.timeInMillis
    return when {
        now < start -> SessionStatus.UPCOMING
        now < end -> SessionStatus.IN_PROGRESS
        else -> SessionStatus.ENDED
    }
}

private fun courseColor(tone: String): Color = when (tone.lowercase(Locale.ROOT)) {
    "green" -> Color(0xFF2E7D32)
    "purple" -> Color(0xFF6A1B9A)
    "coral", "red" -> Color(0xFFC62828)
    "amber", "orange" -> Color(0xFFEF6C00)
    else -> Color(0xFF1565C0)
}
