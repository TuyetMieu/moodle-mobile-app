package com.example.test.data.repository

import com.example.test.data.model.Assignment
import com.example.test.data.model.AssignmentStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class LearningRulesTest {
    @Test
    fun completionTakesPrecedenceOverDeadline() {
        assertEquals(
            AssignmentStatus.DONE,
            LearningRules.status(dueAt = 50L, completedAt = 40L, now = 100L)
        )
    }

    @Test
    fun deadlineStatusUsesTwentyFourHourThreshold() {
        val now = 1_000_000L
        assertEquals(AssignmentStatus.OVERDUE, LearningRules.status(now, null, now))
        assertEquals(AssignmentStatus.URGENT, LearningRules.status(now + DAY, null, now))
        assertEquals(AssignmentStatus.UPCOMING, LearningRules.status(now + DAY + 1L, null, now))
    }

    @Test
    fun filtersByCourseAndSearchAndSortsByDueDate() {
        val assignments = listOf(
            assignment("later", "bio", "Lab report", "Biology", 500L),
            assignment("first", "web", "Project", "Web Development", 200L),
            assignment("matching", "web", "Web quiz", "Web Development", 300L)
        )

        val result = LearningRules.filterAssignments(
            assignments = assignments,
            filter = null,
            courseId = "web",
            query = "quiz",
            now = 100L
        )

        assertEquals(listOf("matching"), result.map { it.id })
    }

    @Test
    fun doneAssignmentsAreSortedByMostRecentCompletion() {
        val assignments = listOf(
            assignment("older", "web", "Older", "Web", 300L, completedAt = 20L),
            assignment("newer", "web", "Newer", "Web", 200L, completedAt = 80L)
        )

        val result = LearningRules.filterAssignments(
            assignments = assignments,
            filter = AssignmentStatus.DONE,
            courseId = null,
            query = "",
            now = 100L
        )

        assertEquals(listOf("newer", "older"), result.map { it.id })
    }

    @Test
    fun progressIsBoundedAndHandlesEmptyCourses() {
        assertEquals(0, LearningRules.progress(0, 0))
        assertEquals(0, LearningRules.progress(-1, 10))
        assertEquals(100, LearningRules.progress(12, 10))
    }

    private fun assignment(
        id: String,
        courseId: String,
        title: String,
        courseTitle: String,
        dueAt: Long,
        completedAt: Long? = null
    ) = Assignment(
        id = id,
        courseId = courseId,
        courseTitle = courseTitle,
        courseShortTitle = courseTitle,
        title = title,
        description = "",
        contribution = null,
        dueAt = dueAt,
        completedAt = completedAt,
        score = null,
        maxScore = 20.0,
        status = if (completedAt != null) AssignmentStatus.DONE else AssignmentStatus.UPCOMING
    )

    private companion object {
        const val DAY = 24 * 60 * 60 * 1000L
    }
}
