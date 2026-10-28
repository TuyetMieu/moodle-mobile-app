package com.example.test.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class ApiException(
    val status: Int,
    val code: String,
    override val message: String
) : IOException(message)

class MoodleApi(
    private val baseUrl: String,
    private val tokenProvider: () -> String? = { null },
    private val connectTimeoutMs: Int = DEFAULT_TIMEOUT_MS,
    private val readTimeoutMs: Int = DEFAULT_TIMEOUT_MS
) {
    suspend fun get(path: String): JSONObject = request("GET", path)

    suspend fun post(path: String, body: JSONObject? = null): JSONObject =
        request("POST", path, body)

    suspend fun delete(path: String): JSONObject = request("DELETE", path)

    private suspend fun request(
        method: String,
        path: String,
        body: JSONObject? = null
    ): JSONObject = withContext(Dispatchers.IO) {
        require(path.startsWith('/')) { "API path must start with '/'" }

        val connection = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs
            connection.setRequestProperty("Accept", "application/json")
            tokenProvider()?.takeIf(String::isNotBlank)?.let {
                connection.setRequestProperty("Authorization", "Bearer $it")
            }

            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { output ->
                    output.write(body.toString().toByteArray(Charsets.UTF_8))
                }
            }

            val status = connection.responseCode
            if (status !in 200..299) throw readError(connection, status)

            val text = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (text.isBlank()) JSONObject() else JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }

    private fun readError(connection: HttpURLConnection, status: Int): ApiException {
        val fallbackCode = "http_$status"
        val fallbackMessage = "Server returned HTTP $status"
        val text = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            .orEmpty()
        val error = runCatching { JSONObject(text).optJSONObject("error") }.getOrNull()
        val code = error?.optString("code")?.takeIf(String::isNotBlank) ?: fallbackCode
        val message = error?.optString("message")?.takeIf(String::isNotBlank) ?: fallbackMessage
        return ApiException(status, code, message)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 10_000
    }
}
