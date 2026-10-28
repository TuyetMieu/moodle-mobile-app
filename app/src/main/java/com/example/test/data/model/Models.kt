package com.example.test.data.model

enum class AssignmentStatus {
    DONE,
    OVERDUE,
    URGENT,
    UPCOMING
}

data class Assignment(
    val id: String,
    val courseId: String,
    val courseTitle: String,
    val courseShortTitle: String,
    val title: String,
    val description: String,
    val contribution: Double?,
    val dueAt: Long,
    val completedAt: Long?,
    val score: Double?,
    val maxScore: Double,
    val status: AssignmentStatus
) {
    val completed: Boolean
        get() = completedAt != null
}

data class Course(
    val id: String,
    val title: String,
    val shortTitle: String,
    val code: String,
    val tone: String,
    val credits: Int,
    val done: Int,
    val total: Int
) {
    val progress: Int
        get() = if (total == 0) 0 else done * 100 / total
}

data class ClassSession(
    val id: String,
    val courseId: String,
    val weekday: Int,
    val start: String,
    val end: String,
    val kind: String,
    val room: String
)

data class LearningData(
    val profileName: String,
    val courses: List<Course>,
    val assignments: List<Assignment>,
    val sessions: List<ClassSession>,
    val semesterStartsAt: Long,
    val semesterEndsAt: Long
)
