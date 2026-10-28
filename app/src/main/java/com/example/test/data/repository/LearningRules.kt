package com.example.test.data.repository

import com.example.test.data.model.Assignment
import com.example.test.data.model.AssignmentStatus

object LearningRules {
    fun status(assignment: Assignment, now: Long): AssignmentStatus =
        status(assignment.dueAt, assignment.completedAt, now)

    fun status(dueAt: Long, completedAt: Long?, now: Long): AssignmentStatus =
        when {
            completedAt != null -> AssignmentStatus.DONE
            dueAt <= now -> AssignmentStatus.OVERDUE
            dueAt - now <= 24 * 60 * 60 * 1000L -> AssignmentStatus.URGENT
            else -> AssignmentStatus.UPCOMING
        }

    fun filterAssignments(
        assignments: List<Assignment>,
        filter: AssignmentStatus?,
        courseId: String?,
        query: String,
        now: Long
    ): List<Assignment> {
        val normalizedQuery = query.trim()
        val filtered = assignments.filter { assignment ->
            val matchesCourse = courseId == null || assignment.courseId == courseId
            val matchesQuery = normalizedQuery.isEmpty() ||
                assignment.title.contains(normalizedQuery, ignoreCase = true) ||
                assignment.courseTitle.contains(normalizedQuery, ignoreCase = true) ||
                assignment.courseShortTitle.contains(normalizedQuery, ignoreCase = true)
            val status = status(assignment, now)
            val matchesStatus = when (filter) {
                null -> true
                AssignmentStatus.DONE -> status == AssignmentStatus.DONE
                AssignmentStatus.URGENT -> status == AssignmentStatus.URGENT ||
                    status == AssignmentStatus.OVERDUE
                AssignmentStatus.OVERDUE -> status == AssignmentStatus.OVERDUE
                AssignmentStatus.UPCOMING -> status == AssignmentStatus.UPCOMING
            }
            matchesCourse && matchesQuery && matchesStatus
        }
        return if (filter == AssignmentStatus.DONE) {
            filtered.sortedByDescending { it.completedAt ?: Long.MIN_VALUE }
        } else {
            filtered.sortedBy { it.dueAt }
        }
    }

    fun progress(done: Int, total: Int): Int =
        if (total <= 0) 0 else (done.coerceIn(0, total) * 100 / total)
}
