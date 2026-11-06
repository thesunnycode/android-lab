package com.example.lab12httpwebservicesdemo

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** One HTTP exchange, kept so the inspector can show exactly what went over the wire. */
data class HttpResult(
    val method: String,
    val url: String,
    val requestBody: String?,
    val code: Int,
    val message: String,
    val contentType: String?,
    val body: String,
    val elapsedMs: Long,
) {
    val isSuccess get() = code in 200..299
    val sizeBytes get() = body.toByteArray(Charsets.UTF_8).size
}

/**
 * A minimal HTTP client built on [HttpURLConnection], the HTTP API that ships with Android.
 *
 * Calls block, so they must run on a background thread: Android throws
 * `NetworkOnMainThreadException` if the main (UI) thread opens a connection.
 */
object HttpClient {
    private const val TIMEOUT_MS = 15_000

    @Throws(IOException::class)
    fun get(url: String): HttpResult = execute("GET", url, null)

    @Throws(IOException::class)
    fun postJson(url: String, json: String): HttpResult = execute("POST", url, json)

    private fun execute(method: String, url: String, json: String?): HttpResult {
        val started = System.nanoTime()
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "TripCast/1.0 (Android lab)")
            if (json != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connection.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            }
            // Reading the status code sends the request and waits for the response headers.
            val code = connection.responseCode
            // 4xx/5xx bodies arrive on the error stream, not the input stream.
            val stream = if (code >= 400) connection.errorStream else connection.inputStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            return HttpResult(
                method = method,
                url = url,
                requestBody = json,
                code = code,
                message = connection.responseMessage.orEmpty(),
                contentType = connection.contentType,
                body = body,
                elapsedMs = (System.nanoTime() - started) / 1_000_000,
            )
        } finally {
            connection.disconnect()
        }
    }
}
