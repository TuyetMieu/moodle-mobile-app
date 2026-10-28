package com.example.test.data.remote

import com.example.test.data.model.Assignment
import com.example.test.data.model.AssignmentStatus
import com.example.test.data.model.ClassSession
import com.example.test.data.model.Course
import com.example.test.data.model.LearningData
import org.json.JSONObject

class JsonMapper {
    fun learningData(root: JSONObject): LearningData {
        val timetable = root.optJSONObject("timetable") ?: JSONObject()
        val profile = root.optJSONObject("profile") ?: JSONObject()
        return LearningData(
            profileName = profile.optString("shortName")
                .takeIf(String::isNotBlank)
                ?: profile.optString("fullName"),
            courses = root.optJSONArray("courses").toObjects(::course),
            assignments = root.optJSONArray("assignments").toObjects(::assignment),
            sessions = timetable.optJSONArray("sessions").toObjects(::session),
            semesterStartsAt = timetable.optLong("semesterStartsAt"),
            semesterEndsAt = timetable.optLong("semesterEndsAt")
        )
    }

    fun assignment(json: JSONObject): Assignment = Assignment(
        id = json.getString("id"),
        courseId = json.getString("courseId"),
        courseTitle = json.optString("courseTitle"),
        courseShortTitle = json.optString("courseShortTitle"),
        title = json.optString("title"),
        description = json.optString("description"),
        contribution = json.nullableDouble("contribution"),
        dueAt = json.optLong("dueAt"),
        completedAt = json.nullableLong("completedAt"),
        score = json.nullableDouble("score"),
        maxScore = json.optDouble("maxScore", 20.0),
        status = runCatching {
            AssignmentStatus.valueOf(json.optString("status", "UPCOMING"))
        }.getOrDefault(AssignmentStatus.UPCOMING)
    )

    private fun course(json: JSONObject) = Course(
        id = json.getString("id"),
        title = json.optString("title"),
        shortTitle = json.optString("shortTitle", json.optString("title")),
        code = json.optString("code"),
        tone = json.optString("tone", "blue"),
        credits = json.optInt("credits"),
        done = json.optInt("done"),
        total = json.optInt("total")
    )

    private fun session(json: JSONObject) = ClassSession(
        id = json.getString("id"),
        courseId = json.getString("courseId"),
        weekday = json.optInt("weekday"),
        start = json.optString("start"),
        end = json.optString("end"),
        kind = json.optString("kind"),
        room = json.optString("room")
    )

    private inline fun <T> org.json.JSONArray?.toObjects(transform: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        return List(length()) { index -> transform(getJSONObject(index)) }
    }

    private fun JSONObject.nullableLong(key: String): Long? =
        if (isNull(key) || !has(key)) null else optLong(key)

    private fun JSONObject.nullableDouble(key: String): Double? =
        if (isNull(key) || !has(key)) null else optDouble(key)
}
