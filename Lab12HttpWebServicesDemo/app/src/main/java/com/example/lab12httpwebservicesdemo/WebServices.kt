package com.example.lab12httpwebservicesdemo

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

data class City(
    val name: String,
    val region: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
) {
    val label get() = listOf(region, country).filter { it.isNotBlank() }.joinToString(", ")
}

data class Day(val date: LocalDate, val code: Int, val maxC: Double, val minC: Double, val rainChance: Int)

data class Forecast(
    val time: String,
    val temperatureC: Double,
    val feelsLikeC: Double,
    val humidity: Int,
    val windKmh: Double,
    val code: Int,
    val days: List<Day>,
)

data class CheckIn(val name: String, val usn: String, val destination: String) {
    fun toJson(): String = JSONObject()
        .put("name", name)
        .put("usn", usn)
        .put("destination", destination)
        .put("purpose", "Field-trip check-in")
        .toString()
}

/** What the server sent back after the POST: the record it stored, with the id it assigned. */
data class Receipt(val id: Int, val name: String, val usn: String, val destination: String)

class CityNotFoundException(val query: String) : Exception("No city matches \"$query\"")

/**
 * The REST endpoints TripCast talks to and the JSON they return.
 *
 * - Open-Meteo geocoding and forecast APIs: free, keyless GET endpoints returning JSON.
 * - JSONPlaceholder: a public fake REST API that accepts a POST and answers `201 Created`.
 */
object WebServices {
    const val GEOCODING = "https://geocoding-api.open-meteo.com/v1/search"
    const val FORECAST = "https://api.open-meteo.com/v1/forecast"
    const val CHECK_IN = "https://jsonplaceholder.typicode.com/posts"

    fun geocodeUrl(city: String): String =
        "$GEOCODING?name=${encode(city.trim())}&count=1&language=en&format=json"

    fun forecastUrl(city: City): String = FORECAST +
        "?latitude=${city.latitude}&longitude=${city.longitude}" +
        "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m" +
        "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
        "&timezone=auto&forecast_days=5"

    /** Returns the best match, or throws [CityNotFoundException] when `results` is missing or empty. */
    @Throws(JSONException::class, CityNotFoundException::class)
    fun parseCity(query: String, json: String): City {
        val results = JSONObject(json).optJSONArray("results")
        if (results == null || results.length() == 0) throw CityNotFoundException(query.trim())
        val first = results.getJSONObject(0)
        return City(
            name = first.getString("name"),
            region = first.optString("admin1"),
            country = first.optString("country"),
            latitude = first.getDouble("latitude"),
            longitude = first.getDouble("longitude"),
        )
    }

    @Throws(JSONException::class)
    fun parseForecast(json: String): Forecast {
        val root = JSONObject(json)
        val current = root.getJSONObject("current")
        val daily = root.getJSONObject("daily")
        val dates = daily.getJSONArray("time")
        val days = (0 until dates.length()).map { i ->
            Day(
                date = LocalDate.parse(dates.getString(i)),
                code = daily.getJSONArray("weather_code").getInt(i),
                maxC = daily.getJSONArray("temperature_2m_max").getDouble(i),
                minC = daily.getJSONArray("temperature_2m_min").getDouble(i),
                rainChance = daily.optJSONArray("precipitation_probability_max").intOrZero(i),
            )
        }
        return Forecast(
            time = current.getString("time"),
            temperatureC = current.getDouble("temperature_2m"),
            feelsLikeC = current.getDouble("apparent_temperature"),
            humidity = current.getInt("relative_humidity_2m"),
            windKmh = current.getDouble("wind_speed_10m"),
            code = current.getInt("weather_code"),
            days = days,
        )
    }

    @Throws(JSONException::class)
    fun parseReceipt(json: String): Receipt {
        val root = JSONObject(json)
        return Receipt(
            id = root.getInt("id"),
            name = root.getString("name"),
            usn = root.getString("usn"),
            destination = root.optString("destination"),
        )
    }

    /** Human-readable label for a WMO weather interpretation code, as used by Open-Meteo. */
    fun describe(code: Int): String = when (code) {
        0 -> "Clear sky"
        1 -> "Mainly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51, 53, 55 -> "Drizzle"
        56, 57 -> "Freezing drizzle"
        61, 63, 65 -> "Rain"
        66, 67 -> "Freezing rain"
        71, 73, 75, 77 -> "Snow"
        80, 81, 82 -> "Rain showers"
        85, 86 -> "Snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm with hail"
        else -> "Unknown"
    }

    fun icon(code: Int): String = when (code) {
        0, 1 -> "☀️"
        2 -> "⛅"
        3 -> "☁️"
        45, 48 -> "🌫️"
        51, 53, 55, 56, 57, 80, 81, 82 -> "🌦️"
        61, 63, 65, 66, 67 -> "🌧️"
        71, 73, 75, 77, 85, 86 -> "❄️"
        95, 96, 99 -> "⛈️"
        else -> "🌡️"
    }

    /** A packing tip for the trip, derived from the forecast the server returned. */
    fun advice(forecast: Forecast): String {
        val rain = forecast.days.firstOrNull()?.rainChance ?: 0
        return when {
            forecast.code >= 95 -> "Thunderstorms expected: plan indoor stops."
            rain >= 60 || forecast.code in 61..82 -> "High chance of rain: carry an umbrella."
            forecast.temperatureC >= 33 -> "Hot day: carry water and a cap."
            forecast.temperatureC <= 12 -> "Cold day: pack a warm jacket."
            else -> "Good weather for the trip."
        }
    }

    fun degrees(value: Double) = "${value.roundToInt()}°"

    fun weekday(date: LocalDate, today: LocalDate): String =
        if (date == today) "Today" else date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)

    /** Indents a JSON body for the inspector; anything that is not JSON is returned unchanged. */
    fun pretty(body: String): String = try {
        val trimmed = body.trim()
        when {
            trimmed.startsWith("{") -> JSONObject(trimmed).toString(2)
            trimmed.startsWith("[") -> JSONArray(trimmed).toString(2)
            else -> body
        }.replace("\\/", "/")
    } catch (_: JSONException) {
        body
    }

    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")

    private fun JSONArray?.intOrZero(i: Int) = if (this == null || isNull(i)) 0 else getInt(i)
}
