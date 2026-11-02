package com.example.lab11googlemapsdemo

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var map: WebView
    private lateinit var mapProgress: LinearProgressIndicator
    private lateinit var mapOffline: View
    private lateinit var trailSummary: TextView
    private lateinit var status: TextView
    private lateinit var clearTrail: MaterialButton
    private lateinit var placeScroller: HorizontalScrollView
    private lateinit var placeList: LinearLayout

    private val cards = mutableMapOf<String, View>()
    private val trail = mutableListOf<Place>()
    /** A place the user tapped; shown instead of the route until the trail or the view changes. */
    private var focus: Place? = null
    private var filter: Category? = null
    private var mapType = MapType.MAP
    private var shownUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = true
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }
        map = findViewById(R.id.map)
        mapProgress = findViewById(R.id.mapProgress)
        mapOffline = findViewById(R.id.mapOffline)
        trailSummary = findViewById(R.id.trailSummary)
        status = findViewById(R.id.status)
        clearTrail = findViewById(R.id.clearTrail)
        placeScroller = findViewById(R.id.placeScroller)
        placeList = findViewById(R.id.placeList)
        // Clip the web map to the window's rounded corners.
        findViewById<View>(R.id.mapWindow).clipToOutline = true

        savedInstanceState?.let { state ->
            state.getStringArrayList(KEY_TRAIL)?.mapNotNullTo(trail) { Places.byId(it) }
            focus = state.getString(KEY_FOCUS)?.let { Places.byId(it) }
            filter = state.getString(KEY_FILTER)?.let { Category.valueOf(it) }
            mapType = MapType.entries[state.getInt(KEY_TYPE)]
        }
        setUpMap()
        buildFilters()
        buildCards()
        renderTrail()

        findViewById<View>(R.id.layers).setOnClickListener {
            mapType = MapType.entries[(mapType.ordinal + 1) % MapType.entries.size]
            showMap()
            status.text = getString(R.string.mode_changed, getString(TYPE_LABELS.getValue(mapType)))
        }
        findViewById<View>(R.id.fit).setOnClickListener {
            focus = null
            showMap()
            status.text = describeMap()
        }
        findViewById<View>(R.id.openMaps).setOnClickListener { openInGoogleMaps() }
        findViewById<View>(R.id.retry).setOnClickListener {
            shownUrl = null
            showMap()
        }
        clearTrail.setOnClickListener {
            trail.clear()
            focus = null
            renderTrail()
            status.setText(R.string.trail_cleared)
        }
    }

    // Google's embeddable map is a JavaScript page; no JavaScript interface is exposed to it.
    @SuppressLint("SetJavaScriptEnabled")
    private fun setUpMap() {
        map.settings.javaScriptEnabled = true
        map.settings.domStorageEnabled = true
        map.setBackgroundColor(MAP_BACKGROUND)
        map.webViewClient = object : WebViewClient() {
            // "View larger map" and similar links leave the embed: hand them to the Maps app or browser.
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (!request.isForMainFrame) return false
                openExternally(request.url.toString())
                return true
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                // Only a failure of the map page itself matters; a missing tile or icon is not worth an error.
                val url = request.url.toString()
                if (url.startsWith("https://maps.google.com/maps") || url.startsWith("https://www.google.com/maps/embed")) {
                    mapOffline.visibility = View.VISIBLE
                }
            }
        }
        map.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                mapProgress.setProgressCompat(newProgress, true)
                mapProgress.visibility = if (newProgress < 100) View.VISIBLE else View.INVISIBLE
            }
        }
    }

    /** Loads the Google Map for the current state: a focused place, the walking route, or the city overview. */
    private fun showMap() {
        val url = GoogleMapsUrls.embed(trail, focus, mapType)
        if (url == shownUrl) return
        shownUrl = url
        mapOffline.visibility = View.GONE
        // The embed only renders inside an iframe, so wrap it in a minimal full-size page.
        val page = """
            <!DOCTYPE html><html><head><meta name="viewport" content="width=device-width, initial-scale=1">
            <style>html,body{margin:0;height:100%;background:#F3EBDD}iframe{border:0;width:100%;height:100%;display:block}</style>
            </head><body><iframe src="$url" allowfullscreen loading="eager"></iframe></body></html>
        """.trimIndent()
        map.loadDataWithBaseURL(BASE_URL, page, "text/html", "UTF-8", null)
    }

    private fun describeMap(): String {
        val place = focus ?: trail.singleOrNull()
        return when {
            place != null -> getString(R.string.showing_place, place.name)
            trail.size >= 2 -> resources.getQuantityString(R.plurals.showing_route, trail.size, trail.size)
            else -> getString(R.string.showing_overview)
        }
    }

    private fun openInGoogleMaps() = openExternally(GoogleMapsUrls.openInApp(trail, focus))

    private fun openExternally(url: String) {
        try {
            // Opens the Google Maps app when it is installed, otherwise the browser.
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.no_maps_app, Toast.LENGTH_LONG).show()
        }
    }

    private fun buildFilters() {
        val group = findViewById<ChipGroup>(R.id.filters)
        val options = listOf<Category?>(null) + Category.entries
        options.forEach { category ->
            val chip = layoutInflater.inflate(R.layout.chip_filter, group, false) as Chip
            chip.id = View.generateViewId()
            chip.text = category?.label ?: getString(R.string.filter_all)
            chip.chipIconTint = ColorStateList.valueOf(category?.color ?: ACCENT)
            chip.isChecked = category == filter
            chip.tag = category
            group.addView(chip)
        }
        group.setOnCheckedStateChangeListener { _, checked ->
            filter = checked.firstOrNull()?.let { group.findViewById<Chip>(it).tag as Category? }
            applyFilter()
            placeScroller.smoothScrollTo(0, 0)
        }
    }

    private fun buildCards() {
        Places.all.forEach { place ->
            val card = layoutInflater.inflate(R.layout.row_place, placeList, false)
            card.findViewById<TextView>(R.id.category).apply {
                text = place.category.label.uppercase(Locale.ROOT)
                backgroundTintList = ColorStateList.valueOf(place.category.color)
            }
            card.findViewById<TextView>(R.id.placeName).text = place.name
            card.findViewById<TextView>(R.id.placeNote).text = place.note
            card.setOnClickListener {
                focus = place
                showMap()
                status.text = describeMap()
            }
            card.findViewById<View>(R.id.toggleStop).setOnClickListener { toggleStop(place) }
            cards[place.id] = card
            placeList.addView(card)
        }
    }

    private fun toggleStop(place: Place) {
        // A trail change returns the map from a single focused place to the route.
        focus = null
        if (trail.remove(place)) status.text = getString(R.string.removed, place.name)
        else {
            trail.add(place)
            status.text = getString(R.string.added, place.name, trail.size)
        }
        renderTrail()
    }

    /** Syncs the trail into the summary bar, every card and the map. */
    private fun renderTrail() {
        val length = TrailMath.lengthMeters(trail)
        trailSummary.text = if (trail.isEmpty()) getString(R.string.trail_empty)
        else resources.getQuantityString(R.plurals.trail_summary, trail.size, trail.size,
            TrailMath.formatDistance(length), TrailMath.formatDuration(TrailMath.walkingMinutes(length)))
        clearTrail.visibility = if (trail.isEmpty()) View.GONE else View.VISIBLE

        Places.all.forEach { place ->
            val stop = trail.indexOf(place)
            val card = cards.getValue(place.id)
            card.setBackgroundResource(if (stop >= 0) R.drawable.bg_place_card_active else R.drawable.bg_place_card)
            card.findViewById<TextView>(R.id.stopBadge).apply {
                visibility = if (stop >= 0) View.VISIBLE else View.GONE
                text = getString(R.string.stop_badge, stop + 1)
            }
            card.findViewById<MaterialButton>(R.id.toggleStop).apply {
                setText(if (stop >= 0) R.string.added_stop else R.string.add_stop)
                setIconResource(if (stop >= 0) R.drawable.ic_check else R.drawable.ic_add)
            }
            card.findViewById<TextView>(R.id.placeDistance).text = when {
                stop == 0 -> getString(R.string.trail_start)
                stop > 0 -> getString(R.string.leg_from, TrailMath.formatDistance(
                    TrailMath.distanceMeters(trail[stop - 1], place)), trail[stop - 1].name)
                trail.isNotEmpty() -> getString(R.string.from_last_stop,
                    TrailMath.formatDistance(TrailMath.distanceMeters(trail.last(), place)), trail.size)
                else -> getString(R.string.coords, place.lat, place.lng)
            }
            card.contentDescription = listOfNotNull(place.name, place.category.label,
                if (stop >= 0) getString(R.string.stop_number, stop + 1) else null).joinToString(", ")
        }
        applyFilter()
        showMap()
    }

    private fun applyFilter() {
        Places.all.forEach { place ->
            // Trail stops stay listed under any filter so the route's stops are always reachable.
            val visible = filter == null || place.category == filter || place in trail
            cards[place.id]?.visibility = if (visible) View.VISIBLE else View.GONE
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(KEY_TRAIL, ArrayList(trail.map { it.id }))
        outState.putString(KEY_FOCUS, focus?.id)
        outState.putString(KEY_FILTER, filter?.name)
        outState.putInt(KEY_TYPE, mapType.ordinal)
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        map.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        map.destroy()
        super.onDestroy()
    }

    private companion object {
        const val KEY_TRAIL = "trail"
        const val KEY_FOCUS = "focus"
        const val KEY_FILTER = "filter"
        const val KEY_TYPE = "type"
        // Gives the wrapper page an https origin (Android's reserved local-content host) rather than a null one.
        const val BASE_URL = "https://appassets.androidplatform.net/"
        const val ACCENT = 0xFFE8A33D.toInt()
        const val MAP_BACKGROUND = 0xFFF3EBDD.toInt()
        val TYPE_LABELS = mapOf(MapType.MAP to R.string.mode_map, MapType.SATELLITE to R.string.mode_satellite,
            MapType.TERRAIN to R.string.mode_terrain)
    }
}
