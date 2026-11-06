package com.example.lab12httpwebservicesdemo

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONException
import java.io.IOException
import java.time.LocalDate
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.roundToInt

// The UI text is English-only and built from server data, so it is not moved into string resources.
@SuppressLint("SetTextI18n")
class MainActivity : AppCompatActivity() {
    /** All network calls run here; results are posted back to the main thread through [main]. */
    private val network: ExecutorService = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    private lateinit var mode: MaterialButtonToggleGroup
    private lateinit var getPanel: View
    private lateinit var postPanel: View
    private lateinit var cityLayout: TextInputLayout
    private lateinit var city: TextInputEditText
    private lateinit var fetch: MaterialButton
    private lateinit var weatherCard: View
    private lateinit var receiptCard: View
    private lateinit var errorCard: View
    private lateinit var nameLayout: TextInputLayout
    private lateinit var usnLayout: TextInputLayout
    private lateinit var destinationLayout: TextInputLayout
    private lateinit var send: MaterialButton
    private lateinit var progress: LinearProgressIndicator

    private var busy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val header = findViewById<View>(R.id.header)
        val scrim = findViewById<View>(R.id.statusScrim)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.screen)) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            header.updatePadding(top = bars.top)
            scrim.updateLayoutParams { height = bars.top }
            findViewById<View>(R.id.root).updatePadding(bottom = bars.bottom + 24.dp)
            insets
        }

        mode = findViewById(R.id.mode)
        getPanel = findViewById(R.id.getPanel)
        postPanel = findViewById(R.id.postPanel)
        cityLayout = findViewById(R.id.cityLayout)
        city = findViewById(R.id.city)
        fetch = findViewById(R.id.fetch)
        weatherCard = findViewById(R.id.weatherCard)
        receiptCard = findViewById(R.id.receiptCard)
        errorCard = findViewById(R.id.errorCard)
        nameLayout = findViewById(R.id.nameLayout)
        usnLayout = findViewById(R.id.usnLayout)
        destinationLayout = findViewById(R.id.destinationLayout)
        send = findViewById(R.id.send)
        progress = findViewById(R.id.progress)

        mode.addOnButtonCheckedListener { _, id, checked ->
            if (!checked) return@addOnButtonCheckedListener
            getPanel.isVisible = id == R.id.modeGet
            postPanel.isVisible = id == R.id.modePost
            errorCard.isVisible = false
        }
        savedInstanceState?.getInt(KEY_MODE)?.let { mode.check(it) }

        fetch.setOnClickListener { fetchForecast(city.text.toString()) }
        city.setOnEditorActionListener { _, action, _ ->
            (action == EditorInfo.IME_ACTION_SEARCH).also { if (it) fetchForecast(city.text.toString()) }
        }
        buildCityChips()

        send.setOnClickListener { sendCheckIn() }
        findViewById<TextView>(R.id.destination).setOnEditorActionListener { _, action, _ ->
            (action == EditorInfo.IME_ACTION_SEND).also { if (it) sendCheckIn() }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_MODE, mode.checkedButtonId)
    }

    override fun onDestroy() {
        network.shutdownNow()
        super.onDestroy()
    }

    private fun buildCityChips() {
        val group = findViewById<ChipGroup>(R.id.cities)
        QUICK_CITIES.forEach { name ->
            val chip = Chip(this, null, com.google.android.material.R.attr.chipStyle).apply {
                text = name
                chipBackgroundColor = ColorStateList.valueOf(Color.WHITE)
                chipStrokeColor = ColorStateList.valueOf("#DCE4EF".toColorInt())
                chipStrokeWidth = 1f * resources.displayMetrics.density
                setTextColor("#13294B".toColorInt())
                setOnClickListener {
                    city.setText(name)
                    fetchForecast(name)
                }
            }
            group.addView(chip)
        }
    }

    // ---- GET: geocode the city, then fetch its forecast -------------------------------------

    private fun fetchForecast(query: String) {
        if (busy) return
        if (query.isBlank()) {
            cityLayout.error = "Enter a city name"
            return
        }
        cityLayout.error = null
        hideKeyboard()
        startLoading("GET", WebServices.geocodeUrl(query))
        network.execute {
            var last: HttpResult? = null
            try {
                // Request 1: city name -> coordinates.
                val geo = HttpClient.get(WebServices.geocodeUrl(query)).also { last = it }
                if (!geo.isSuccess) throw HttpStatusException(geo)
                val place = WebServices.parseCity(query, geo.body)
                // Request 2: coordinates -> current weather and 5-day forecast.
                val weather = HttpClient.get(WebServices.forecastUrl(place)).also { last = it }
                if (!weather.isSuccess) throw HttpStatusException(weather)
                val forecast = WebServices.parseForecast(weather.body)
                onMain {
                    showInspector(weather, "Request 2 of 2 · forecast")
                    showForecast(place, forecast)
                }
            } catch (e: CityNotFoundException) {
                onMain {
                    last?.let { showInspector(it, "Request 1 of 2 · geocoding") }
                    weatherCard.isVisible = false
                    showError(
                        "City not found",
                        "The server answered 200 OK, but its JSON has no \"results\" for \"${e.query}\". " +
                            "Check the spelling and try again.",
                    )
                }
            } catch (e: Exception) {
                onMain {
                    last?.let { showInspector(it, null) } ?: showInspectorFailure("GET", WebServices.geocodeUrl(query))
                    weatherCard.isVisible = false
                    showFailure(e)
                }
            }
        }
    }

    private fun showForecast(place: City, forecast: Forecast) {
        errorCard.isVisible = false
        weatherCard.isVisible = true
        findViewById<TextView>(R.id.cityName).text = place.name
        findViewById<TextView>(R.id.cityRegion).text = place.label
        findViewById<TextView>(R.id.temperature).text = WebServices.degrees(forecast.temperatureC) + "C"
        findViewById<TextView>(R.id.condition).text = WebServices.describe(forecast.code)
        findViewById<TextView>(R.id.icon).text = WebServices.icon(forecast.code)
        findViewById<TextView>(R.id.feelsLike).text = stat("Feels like", WebServices.degrees(forecast.feelsLikeC))
        findViewById<TextView>(R.id.humidity).text = stat("Humidity", "${forecast.humidity}%")
        findViewById<TextView>(R.id.wind).text = stat("Wind", "${forecast.windKmh.roundToInt()} km/h")
        findViewById<TextView>(R.id.advice).text = "🎒  " + WebServices.advice(forecast)

        val days = findViewById<LinearLayout>(R.id.days)
        days.removeAllViews()
        val today = forecast.days.firstOrNull()?.date ?: LocalDate.now()
        forecast.days.forEachIndexed { i, day ->
            days.addView(TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    if (i > 0) marginStart = 6.dp
                }
                setBackgroundResource(R.drawable.bg_day)
                gravity = Gravity.CENTER
                setPadding(2.dp, 10.dp, 2.dp, 10.dp)
                setTextColor("#13294B".toColorInt())
                textSize = 12f
                setLineSpacing(3f, 1f)
                text = SpannableStringBuilder()
                    .append(WebServices.weekday(day.date, today)).append('\n')
                    .append(WebServices.icon(day.code), RelativeSizeSpan(1.9f), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    .append('\n')
                    .append("${WebServices.degrees(day.maxC)} / ${WebServices.degrees(day.minC)}\n💧${day.rainChance}%")
            })
        }
        // Carry the destination over to the check-in form.
        findViewById<TextView>(R.id.destination).text = place.name
    }

    private fun stat(label: String, value: String) = "$label\n$value"

    // ---- POST: send the student's check-in as JSON ------------------------------------------

    private fun sendCheckIn() {
        if (busy) return
        val name = text(R.id.name)
        val usn = text(R.id.usn).uppercase(Locale.ROOT)
        val destination = text(R.id.destination)
        nameLayout.error = if (name.isEmpty()) "Name is required" else null
        usnLayout.error = when {
            usn.isEmpty() -> "USN is required"
            !USN_PATTERN.matches(usn) -> "Use the format 25MCAR0230"
            else -> null
        }
        destinationLayout.error = if (destination.isEmpty()) "Destination is required" else null
        if (nameLayout.error != null || usnLayout.error != null || destinationLayout.error != null) return

        hideKeyboard()
        val checkIn = CheckIn(name, usn, destination)
        startLoading("POST", WebServices.CHECK_IN)
        network.execute {
            var result: HttpResult? = null
            try {
                result = HttpClient.postJson(WebServices.CHECK_IN, checkIn.toJson())
                if (!result.isSuccess) throw HttpStatusException(result)
                val receipt = WebServices.parseReceipt(result.body)
                val shown = result
                onMain {
                    showInspector(shown, "Request 1 of 1 · check-in")
                    showReceipt(receipt, shown)
                }
            } catch (e: Exception) {
                val shown = result
                onMain {
                    shown?.let { showInspector(it, null) } ?: showInspectorFailure("POST", WebServices.CHECK_IN)
                    receiptCard.isVisible = false
                    showFailure(e)
                }
            }
        }
    }

    private fun showReceipt(receipt: Receipt, result: HttpResult) {
        errorCard.isVisible = false
        receiptCard.isVisible = true
        findViewById<TextView>(R.id.receiptTitle).text = "✅  Check-in #${receipt.id} confirmed"
        findViewById<TextView>(R.id.receiptBody).text =
            "Server replied ${result.code} ${result.message} and stored:\n" +
                "Name  ·  ${receipt.name}\n" +
                "USN  ·  ${receipt.usn}\n" +
                "Trip  ·  ${receipt.destination}"
    }

    // ---- Shared UI --------------------------------------------------------------------------

    private fun startLoading(method: String, url: String) {
        busy = true
        fetch.isEnabled = false
        send.isEnabled = false
        progress.isVisible = true
        errorCard.isVisible = false
        findViewById<TextView>(R.id.method).apply { isVisible = true; text = method }
        findViewById<TextView>(R.id.statusCode).isVisible = false
        findViewById<TextView>(R.id.requestUrl).text = url
        findViewById<TextView>(R.id.timing).apply { isVisible = true; text = "Waiting for the server…" }
        findViewById<View>(R.id.requestLabel).isVisible = false
        findViewById<View>(R.id.requestBody).isVisible = false
        findViewById<View>(R.id.responseLabel).isVisible = false
        findViewById<View>(R.id.responseBody).isVisible = false
    }

    private fun stopLoading() {
        busy = false
        fetch.isEnabled = true
        send.isEnabled = true
        progress.isVisible = false
    }

    private fun showInspector(result: HttpResult, step: String?) {
        stopLoading()
        findViewById<TextView>(R.id.method).apply { isVisible = true; text = result.method }
        findViewById<TextView>(R.id.statusCode).apply {
            isVisible = true
            text = "${result.code} ${result.message}".trim()
            val ok = result.isSuccess
            backgroundTintList = ColorStateList.valueOf((if (ok) "#DCFCE7" else "#FEE2E2").toColorInt())
            setTextColor((if (ok) "#166534" else "#991B1B").toColorInt())
        }
        findViewById<TextView>(R.id.requestUrl).text = result.url
        findViewById<TextView>(R.id.timing).apply {
            isVisible = true
            text = listOfNotNull(
                step,
                "${result.elapsedMs} ms",
                formatBytes(result.sizeBytes),
                result.contentType?.substringBefore(';'),
            ).joinToString("  ·  ")
        }
        val hasRequestBody = result.requestBody != null
        findViewById<View>(R.id.requestLabel).isVisible = hasRequestBody
        findViewById<TextView>(R.id.requestBody).apply {
            isVisible = hasRequestBody
            text = result.requestBody?.let { WebServices.pretty(it) }
        }
        findViewById<View>(R.id.responseLabel).isVisible = true
        findViewById<TextView>(R.id.responseBody).apply {
            isVisible = true
            text = WebServices.pretty(result.body).ifEmpty { "(empty body)" }
        }
    }

    private fun showInspectorFailure(method: String, url: String) {
        stopLoading()
        findViewById<TextView>(R.id.method).apply { isVisible = true; text = method }
        findViewById<TextView>(R.id.statusCode).apply {
            isVisible = true
            text = "NO RESPONSE"
            backgroundTintList = ColorStateList.valueOf("#FEE2E2".toColorInt())
            setTextColor("#991B1B".toColorInt())
        }
        findViewById<TextView>(R.id.requestUrl).text = url
        findViewById<TextView>(R.id.timing).text = "The request never reached the server."
    }

    private fun showFailure(e: Exception) = when (e) {
        is HttpStatusException -> showError("Server error ${e.result.code}", "The server rejected the request: ${e.result.message}.")
        is JSONException -> showError("Unexpected response", "The server's reply was not the JSON this app expects.")
        is IOException -> showError("Can't reach the server", "Check that the device is online, then try again. (${e.javaClass.simpleName})")
        else -> showError("Something went wrong", e.message ?: e.javaClass.simpleName)
    }

    private fun showError(title: String, body: String) {
        errorCard.isVisible = true
        findViewById<TextView>(R.id.errorTitle).text = "⚠️  $title"
        findViewById<TextView>(R.id.errorBody).text = body
    }

    private fun onMain(block: () -> Unit) {
        main.post { if (!isDestroyed) block() }
    }

    private fun text(id: Int) = findViewById<TextView>(id).text.toString().trim()

    private fun hideKeyboard() {
        currentFocus?.let {
            getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(it.windowToken, 0)
            it.clearFocus()
        }
    }

    private val Int.dp get() = (this * resources.displayMetrics.density).roundToInt()

    private class HttpStatusException(val result: HttpResult) : Exception("HTTP ${result.code}")

    companion object {
        private const val KEY_MODE = "mode"
        private val QUICK_CITIES = listOf("Bengaluru", "Mysuru", "Madikeri", "Ooty", "Hampi", "Mangaluru")
        private val USN_PATTERN = Regex("^[0-9]{2}[A-Z]{3,4}[0-9]{3,4}$")

        fun formatBytes(bytes: Int): String =
            if (bytes < 1024) "$bytes B" else String.format(Locale.ENGLISH, "%.1f KB", bytes / 1024.0)
    }
}
