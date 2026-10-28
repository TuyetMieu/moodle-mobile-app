package com.example.test.data.repository

import android.content.Context
import com.example.test.BuildConfig
import com.example.test.data.model.Assignment
import com.example.test.data.model.LearningData
import com.example.test.data.remote.JsonMapper
import com.example.test.data.remote.ApiException
import com.example.test.data.remote.MoodleApi
import org.json.JSONObject
import java.io.IOException

class MoodleRepository(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("moodle_session", Context.MODE_PRIVATE)
    private val api: MoodleApi
        get() = MoodleApi(
            baseUrl = preferences.getString(KEY_BASE_URL, BuildConfig.API_BASE_URL) ?: BuildConfig.API_BASE_URL,
            tokenProvider = { preferences.getString(KEY_TOKEN, null) }
        )
    private val mapper = JsonMapper()

    val isSignedIn: Boolean
        get() = !preferences.getString(KEY_TOKEN, null).isNullOrBlank()

    var isOffline: Boolean = false
        private set

    suspend fun login(baseUrl: String, username: String, password: String) {
        require(baseUrl.startsWith("http://") || baseUrl.startsWith("https://")) {
            "Server URL must start with http:// or https://"
        }
        val normalizedUrl = baseUrl.trimEnd('/')
        val response = MoodleApi(normalizedUrl).post(
            "/api/v1/auth/login",
            JSONObject().put("username", username.trim()).put("password", password)
        )
        val token = response.optString("token").takeIf(String::isNotBlank)
            ?: error("Login response did not contain a token")
        preferences.edit()
            .putString(KEY_BASE_URL, normalizedUrl)
            .putString(KEY_TOKEN, token)
            .apply()
    }

    suspend fun load(): LearningData {
        return try {
            val response = api.get("/api/v1/bootstrap")
            preferences.edit().putString(KEY_BOOTSTRAP_CACHE, response.toString()).apply()
            isOffline = false
            mapper.learningData(response)
        } catch (failure: ApiException) {
            throw failure
        } catch (failure: IOException) {
            val cached = preferences.getString(KEY_BOOTSTRAP_CACHE, null) ?: throw failure
            isOffline = true
            mapper.learningData(JSONObject(cached))
        }
    }

    suspend fun setCompleted(assignmentId: String, completed: Boolean): Assignment {
        val path = "/api/v1/assignments/${encodePathSegment(assignmentId)}/completion"
        val response = if (completed) api.post(path) else api.delete(path)
        return mapper.assignment(response)
    }

    fun logout() {
        isOffline = false
        preferences.edit().remove(KEY_TOKEN).remove(KEY_BOOTSTRAP_CACHE).apply()
    }

    private fun encodePathSegment(value: String): String =
        java.net.URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")

    private companion object {
        const val KEY_BASE_URL = "base_url"
        const val KEY_TOKEN = "token"
        const val KEY_BOOTSTRAP_CACHE = "bootstrap_cache"
    }
}
