package com.example.lab12httpwebservicesdemo

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WebServicesTest {
    @Test
    fun requestUrlsAreEncodedAndComplete() {
        assertEquals(
            "https://geocoding-api.open-meteo.com/v1/search?name=New+Delhi&count=1&language=en&format=json",
            WebServices.geocodeUrl("  New Delhi "),
        )
        val url = WebServices.forecastUrl(City("Mysuru", "Karnataka", "India", 12.29791, 76.63925))
        assertTrue(url.startsWith("https://api.open-meteo.com/v1/forecast?latitude=12.29791&longitude=76.63925"))
        assertTrue(url.contains("&current=temperature_2m,"))
        assertTrue(url.endsWith("&timezone=auto&forecast_days=5"))
    }

    @Test
    fun parsesGeocodingAndForecastResponses() {
        val city = WebServices.parseCity(
            "Mysuru",
            """{"results":[{"name":"Mysuru","latitude":12.29791,"longitude":76.63925,"country":"India","admin1":"Karnataka"}]}""",
        )
        assertEquals(City("Mysuru", "Karnataka", "India", 12.29791, 76.63925), city)
        assertEquals("Karnataka, India", city.label)

        val forecast = WebServices.parseForecast(
            """{"current":{"time":"2026-11-06T12:15","temperature_2m":28.7,"relative_humidity_2m":57,
               "apparent_temperature":31.9,"weather_code":3,"wind_speed_10m":12.8},
               "daily":{"time":["2026-11-06","2026-11-07"],"weather_code":[80,51],
               "temperature_2m_max":[29.6,29.0],"temperature_2m_min":[20.3,20.1],
               "precipitation_probability_max":[65,null]}}""",
        )
        assertEquals(28.7, forecast.temperatureC, 0.0)
        assertEquals(57, forecast.humidity)
        assertEquals(Day(LocalDate.of(2026, 11, 6), 80, 29.6, 20.3, 65), forecast.days[0])
        assertEquals(0, forecast.days[1].rainChance)
        assertEquals("Overcast", WebServices.describe(forecast.code))
        assertEquals("High chance of rain: carry an umbrella.", WebServices.advice(forecast))
        assertEquals("29°", WebServices.degrees(forecast.temperatureC))
    }

    @Test(expected = CityNotFoundException::class)
    fun emptyGeocodingResultMeansCityNotFound() {
        WebServices.parseCity("Xyzzyqq", """{"generationtime_ms":0.09}""")
    }

    @Test
    fun checkInRoundTripsThroughJson() {
        val body = CheckIn("Sunny Kr Singh", "25MCAR0230", "Mysuru").toJson()
        val sent = JSONObject(body)
        assertEquals("25MCAR0230", sent.getString("usn"))
        // JSONPlaceholder echoes the body back with a new id.
        val receipt = WebServices.parseReceipt(sent.put("id", 101).toString())
        assertEquals(Receipt(101, "Sunny Kr Singh", "25MCAR0230", "Mysuru"), receipt)
        assertEquals("812 B", MainActivity.formatBytes(812))
        assertEquals("1.5 KB", MainActivity.formatBytes(1536))
    }
}
