package com.example.test.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.test.BuildConfig
import com.example.test.data.model.Assignment
import com.example.test.data.model.AssignmentStatus
import com.example.test.data.model.LearningData
import com.example.test.data.repository.LearningRules
import com.example.test.data.repository.MoodleRepository
import com.example.test.data.remote.ApiException
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun MoodleApp(repository: MoodleRepository) {
    var signedIn by rememberSaveable { mutableStateOf(repository.isSignedIn) }
    var showTimetable by rememberSaveable { mutableStateOf(false) }
    var data by remember { mutableStateOf<LearningData?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var offline by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            loading = true
            error = null
            try {
                data = repository.load()
                offline = repository.isOffline
            } catch (failure: Exception) {
                if (failure is ApiException && failure.status == 401) {
                    repository.logout()
                    signedIn = false
                }
                error = failure.message ?: "Could not load data from the server."
                offline = false
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(signedIn) {
        if (signedIn && data == null) refresh()
    }

    if (!signedIn) {
        LoginScreen(
            onLogin = { url, username, password ->
                scope.launch {
                    loading = true
                    error = null
                    try {
                        repository.login(url, username, password)
                        signedIn = true
                    } catch (failure: Exception) {
                        error = failure.message ?: "Login failed."
                    } finally {
                        loading = false
                    }
                }
            },
            loading = loading,
            error = error
        )
    } else {
        val logout = {
            repository.logout()
            data = null
            signedIn = false
        }
        if (showTimetable) {
            TimetableScreen(
                data = data,
                loading = loading,
                offline = offline,
                error = error,
                snackbarHostState = snackbarHostState,
                onRetry = ::refresh,
                onShowClasswork = { showTimetable = false },
                onLogout = logout
            )
        } else {
            ClassworkScreen(
                data = data,
                loading = loading,
                offline = offline,
                error = error,
                snackbarHostState = snackbarHostState,
                onRetry = ::refresh,
                onDataChange = { data = it },
                onShowTimetable = { showTimetable = true },
                onLogout = logout
            )
        }
    }
}

@Composable
private fun LoginScreen(
    onLogin: (String, String, String) -> Unit,
    loading: Boolean,
    error: String?
) {
    var url by rememberSaveable { mutableStateOf(BuildConfig.API_BASE_URL) }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Moodle USTH", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(url, { url = it }, label = { Text("Backend URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(username, { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(
            password,
            { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        Button(
            onClick = { onLogin(url, username, password) },
            enabled = !loading && username.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            if (loading) CircularProgressIndicator()
            else Text("Sign in")
        }
    }
}

private enum class WorkFilter(val label: String) {
    TODO("To do"),
    URGENT("Due soon"),
    DONE("Done")
}

@Composable
private fun ClassworkScreen(
    data: LearningData?,
    loading: Boolean,
    offline: Boolean,
    error: String?,
    snackbarHostState: SnackbarHostState,
    onRetry: () -> Unit,
    onDataChange: (LearningData) -> Unit,
    onShowTimetable: () -> Unit,
    onLogout: () -> Unit
) {
    var filter by rememberSaveable { mutableStateOf(WorkFilter.TODO) }
    var selectedCourse by rememberSaveable { mutableStateOf("all") }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedAssignmentId by rememberSaveable { mutableStateOf<String?>(null) }
    var savingAssignmentId by remember { mutableStateOf<String?>(null) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
            now = System.currentTimeMillis()
        }
    }
    LaunchedEffect(actionMessage) {
        actionMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    fun filteredAssignments(option: WorkFilter): List<Assignment> {
        val result = LearningRules.filterAssignments(
            assignments = data?.assignments.orEmpty(),
            filter = when (option) {
                WorkFilter.TODO -> null
                WorkFilter.URGENT -> AssignmentStatus.URGENT
                WorkFilter.DONE -> AssignmentStatus.DONE
            },
            courseId = selectedCourse.takeIf { it != "all" },
            query = query,
            now = now
        )
        return if (option == WorkFilter.TODO) {
            result.filter { LearningRules.status(it, now) != AssignmentStatus.DONE }
        } else {
            result
        }
    }

    val assignments = filteredAssignments(filter)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Classwork", style = MaterialTheme.typography.headlineSmall)
                    Text(data?.let { "Welcome, ${it.profileName}" } ?: "Loading your courses")
                }
                Button(onClick = onShowTimetable) { Text("Timetable") }
                Button(onClick = onLogout) { Text("Sign out") }
            }
            if (loading && data == null) {
                CircularProgressIndicator()
            } else if (data == null) {
                Text(error ?: "No data loaded")
                Button(onClick = onRetry, enabled = !loading) { Text("Retry") }
            } else {
                if (offline) {
                    Text(
                        "Offline · showing saved data",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Button(onClick = onRetry, enabled = !loading) { Text("Retry connection") }
                }
                if (loading && data != null) Text("Refreshing…")
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search assignments") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WorkFilter.entries.forEach { option ->
                        FilterChip(
                            selected = filter == option,
                            onClick = { filter = option },
                            label = { Text("${option.label} (${filteredAssignments(option).size})") }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(selectedCourse == "all", { selectedCourse = "all" }, label = { Text("All courses") })
                    data.courses.forEach { course ->
                        FilterChip(
                            selected = selectedCourse == course.id,
                            onClick = { selectedCourse = course.id },
                            label = { Text(course.shortTitle) }
                        )
                    }
                }
                Text(
                    "${data.assignments.count { LearningRules.status(it, now) == AssignmentStatus.DONE }}/${data.assignments.size} assignments complete",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(assignments, key = { it.id }) { assignment ->
                        AssignmentCard(assignment, now, onClick = { selectedAssignmentId = assignment.id })
                    }
                }
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    Button(onClick = onRetry, enabled = !loading) { Text("Retry") }
                }
            }
        }
    }

    val selectedAssignment = data?.assignments?.firstOrNull { it.id == selectedAssignmentId }
    if (selectedAssignment != null) {
        AlertDialog(
            onDismissRequest = { selectedAssignmentId = null },
            title = { Text(selectedAssignment.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(selectedAssignment.courseTitle)
                    Text(selectedAssignment.description.ifBlank { "No description provided." })
                    Text("Status: ${selectedAssignment.status}")
                    Text("Due: ${DateFormat.getDateTimeInstance().format(Date(selectedAssignment.dueAt))}")
                    selectedAssignment.contribution?.let {
                        Text("How this task counts: ${"%.1f".format(it)}% of the course grade")
                    }
                    selectedAssignment.score?.let {
                        Text("Grade: ${"%.1f".format(it)} / ${"%.1f".format(selectedAssignment.maxScore)}")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = savingAssignmentId == null &&
                        !(selectedAssignment.completed && selectedAssignment.score != null),
                    onClick = {
                        val currentData = data ?: return@TextButton
                        val previous = selectedAssignment
                        val markDone = !previous.completed
                        val optimistic = previous.copy(
                            completedAt = if (markDone) System.currentTimeMillis() else null,
                            status = if (markDone) AssignmentStatus.DONE
                            else LearningRules.status(previous.dueAt, null, now)
                        )
                        onDataChange(
                            currentData.copy(
                                assignments = currentData.assignments.map {
                                    if (it.id == previous.id) optimistic else it
                                }
                            )
                        )
                        savingAssignmentId = previous.id
                        actionMessage = null
                        scope.launch {
                            try {
                                val serverAssignment = repository.setCompleted(previous.id, markDone)
                                val latest = data
                                if (latest != null) {
                                    onDataChange(
                                        latest.copy(
                                            assignments = latest.assignments.map {
                                                if (it.id == serverAssignment.id) serverAssignment else it
                                            }
                                        )
                                    )
                                }
                                actionMessage = if (markDone) "Assignment marked as done." else "Completion undone."
                            } catch (failure: Exception) {
                                val latest = data
                                if (latest != null) {
                                    onDataChange(
                                        latest.copy(
                                            assignments = latest.assignments.map {
                                                if (it.id == previous.id) previous else it
                                            }
                                        )
                                    )
                                }
                                actionMessage = failure.message ?: "Could not update assignment."
                            } finally {
                                savingAssignmentId = null
                            }
                        }
                    }
                ) {
                    Text(if (savingAssignmentId != null) "Saving..." else if (selectedAssignment.completed) "Undo done" else "Mark as done")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedAssignmentId = null }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun AssignmentCard(assignment: Assignment, now: Long, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(assignment.title, style = MaterialTheme.typography.titleMedium)
            Text(assignment.courseShortTitle, style = MaterialTheme.typography.bodyMedium)
            Text("${LearningRules.status(assignment, now)} · ${DateFormat.getDateTimeInstance().format(Date(assignment.dueAt))}")
            assignment.contribution?.let { Text("Contributes ${"%.1f".format(it)}% to course grade") }
        }
    }
}
