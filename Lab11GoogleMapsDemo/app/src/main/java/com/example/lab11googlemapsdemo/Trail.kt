package com.example.lab11googlemapsdemo

import java.net.URLEncoder
import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class Category(val label: String, val glyph: String, val color: Int) {
    HERITAGE("Heritage", "H", 0xFF9C3D1F.toInt()),
    PARKS("Parks", "P", 0xFF2E6B4A.toInt()),
    FOOD("Food", "F", 0xFFC7801A.toInt()),
    CULTURE("Culture", "C", 0xFF3F5A8C.toInt()),
}

/** A stop that can be added to the trail. Plain doubles keep this file free of Android types for unit tests. */
data class Place(
    val id: String,
    val name: String,
    val note: String,
    val category: Category,
    val lat: Double,
    val lng: Double,
)

object Places {
    // Approximate coordinates of well-known central Bengaluru landmarks.
    val all = listOf(
        Place("vidhana", "Vidhana Soudha", "Granite seat of the state legislature, 1956", Category.HERITAGE, 12.9797, 77.5907),
        Place("cubbon", "Cubbon Park", "300 acres of bamboo groves and old trees", Category.PARKS, 12.9763, 77.5929),
        Place("museum", "Visvesvaraya Museum", "Hands-on science and engines since 1962", Category.CULTURE, 12.9752, 77.5963),
        Place("church", "Church Street", "Bookshops, cafés and late-night dosa", Category.FOOD, 12.9750, 77.6030),
        Place("palace", "Bangalore Palace", "Tudor-style palace modelled on Windsor", Category.HERITAGE, 12.9988, 77.5921),
        Place("fort", "Bengaluru Fort", "Stone gateway from the 16th-century mud fort", Category.HERITAGE, 12.9627, 77.5762),
        Place("market", "KR Market", "City's oldest flower and produce market", Category.CULTURE, 12.9647, 77.5776),
        Place("tipu", "Tipu Sultan's Summer Palace", "Teak pillars and painted arches, 1791", Category.HERITAGE, 12.9593, 77.5737),
        Place("mtr", "Mavalli Tiffin Rooms", "Rava idli, filter coffee, since 1924", Category.FOOD, 12.9552, 77.5855),
        Place("lalbagh", "Lalbagh Botanical Garden", "Glass house and 3,000-million-year-old rock", Category.PARKS, 12.9507, 77.5848),
        Place("vvpuram", "VV Puram Food Street", "Evening street of holige and akki rotti", Category.FOOD, 12.9495, 77.5735),
        Place("bull", "Bull Temple", "Monolithic Nandi carved in the 1500s", Category.CULTURE, 12.9426, 77.5683),
    )

    fun byId(id: String) = all.firstOrNull { it.id == id }
}

object TrailMath {
    private const val EARTH_RADIUS_M = 6_371_008.8
    /** Average walking pace used for time estimates: 4.8 km/h. */
    const val WALK_METERS_PER_MINUTE = 80.0

    /** Great-circle distance between two coordinates (haversine formula), in metres. */
    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
        return 2 * EARTH_RADIUS_M * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    fun distanceMeters(a: Place, b: Place) = distanceMeters(a.lat, a.lng, b.lat, b.lng)

    /** Total straight-line length of the trail, walking the stops in order. */
    fun lengthMeters(stops: List<Place>) = stops.zipWithNext { a, b -> distanceMeters(a, b) }.sum()

    fun walkingMinutes(meters: Double) = (meters / WALK_METERS_PER_MINUTE).roundToInt()

    fun formatDistance(meters: Double) =
        if (meters < 1000) "${meters.roundToInt()} m" else String.format(Locale.US, "%.1f km", meters / 1000)

    fun formatDuration(minutes: Int) =
        if (minutes < 60) "$minutes min" else "${minutes / 60} h ${minutes % 60} min"

}

/** Google Maps layers selectable from the layers button; [code] is the `t=` value in Maps URLs. */
enum class MapType(val code: String) { MAP("m"), SATELLITE("h"), TERRAIN("p") }

/**
 * Builds Google Maps URLs. The `output=embed` URLs render Google's embeddable web map, which needs no API key;
 * the `api=1` URLs open the full Google Maps app (or website) with the same place or walking route.
 */
object GoogleMapsUrls {
    private const val EMBED = "https://maps.google.com/maps"
    private const val CENTRE = "12.9680,77.5850" // Central Bengaluru, framing all twelve places.

    fun embed(stops: List<Place>, focus: Place?, type: MapType) = when {
        focus != null -> embedPlace(focus, type)
        stops.size >= 2 -> embedRoute(stops, type)
        stops.size == 1 -> embedPlace(stops[0], type)
        else -> "$EMBED?ll=$CENTRE&z=13&t=${type.code}&hl=en&output=embed"
    }

    fun embedPlace(place: Place, type: MapType) =
        "$EMBED?q=${encode("${place.name}, Bengaluru")}&ll=${coords(place)}&z=16&t=${type.code}&hl=en&output=embed"

    /** Walking directions (`dirflg=w`) from the first stop through every later stop, in order. */
    fun embedRoute(stops: List<Place>, type: MapType) =
        "$EMBED?saddr=${coords(stops.first())}&daddr=${stops.drop(1).joinToString("+to:") { coords(it) }}" +
            "&dirflg=w&t=${type.code}&hl=en&output=embed"

    fun openInApp(stops: List<Place>, focus: Place?): String {
        val place = focus ?: stops.singleOrNull()
        if (place != null) return "https://www.google.com/maps/search/?api=1&query=${encode("${place.name}, Bengaluru")}"
        if (stops.isEmpty()) return "https://www.google.com/maps/@?api=1&map_action=map&center=$CENTRE&zoom=13"
        val waypoints = stops.subList(1, stops.size - 1).joinToString("%7C") { coords(it) }
        return "https://www.google.com/maps/dir/?api=1&origin=${coords(stops.first())}&destination=${coords(stops.last())}" +
            (if (waypoints.isEmpty()) "" else "&waypoints=$waypoints") + "&travelmode=walking"
    }

    private fun coords(place: Place) = String.format(Locale.US, "%.4f,%.4f", place.lat, place.lng)
    private fun encode(text: String) = URLEncoder.encode(text, "UTF-8")
}
