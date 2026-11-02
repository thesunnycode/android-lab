package com.example.lab11googlemapsdemo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrailMathTest {
    private fun place(id: String) = Places.byId(id)!!

    @Test
    fun distanceBetweenNeighbouringLandmarksIsRealistic() {
        // Vidhana Soudha to Cubbon Park is a few hundred metres across the road.
        val metres = TrailMath.distanceMeters(place("vidhana"), place("cubbon"))
        assertTrue("was $metres", metres in 350.0..500.0)
        // Distance is symmetric and zero from a point to itself.
        assertEquals(metres, TrailMath.distanceMeters(place("cubbon"), place("vidhana")), 0.001)
        assertEquals(0.0, TrailMath.distanceMeters(place("lalbagh"), place("lalbagh")), 0.001)
    }

    @Test
    fun trailLengthSumsLegsInOrder() {
        val stops = listOf(place("vidhana"), place("cubbon"), place("museum"))
        val expected = TrailMath.distanceMeters(stops[0], stops[1]) + TrailMath.distanceMeters(stops[1], stops[2])
        assertEquals(expected, TrailMath.lengthMeters(stops), 0.001)
        assertEquals(0.0, TrailMath.lengthMeters(stops.take(1)), 0.0)
        assertEquals(0.0, TrailMath.lengthMeters(emptyList()), 0.0)
        assertEquals(15, TrailMath.walkingMinutes(1200.0))
    }

    @Test
    fun formattingAndGoogleMapsUrls() {
        assertEquals("850 m", TrailMath.formatDistance(850.0))
        assertEquals("2.4 km", TrailMath.formatDistance(2440.0))
        assertEquals("45 min", TrailMath.formatDuration(45))
        assertEquals("1 h 20 min", TrailMath.formatDuration(80))

        val stops = listOf(place("vidhana"), place("cubbon"), place("museum"))
        // Three stops embed as walking directions through every stop, in order, on the chosen layer.
        assertEquals("https://maps.google.com/maps?saddr=12.9797,77.5907&daddr=12.9763,77.5929+to:12.9752,77.5963" +
            "&dirflg=w&t=h&hl=en&output=embed", GoogleMapsUrls.embed(stops, null, MapType.SATELLITE))
        // A focused place wins over the route, and its name is URL-encoded.
        val tipu = GoogleMapsUrls.embed(stops, place("tipu"), MapType.MAP)
        assertTrue(tipu, tipu.startsWith("https://maps.google.com/maps?q=Tipu+Sultan%27s+Summer+Palace%2C+Bengaluru"))
        assertEquals("https://www.google.com/maps/dir/?api=1&origin=12.9797,77.5907&destination=12.9752,77.5963" +
            "&waypoints=12.9763,77.5929&travelmode=walking", GoogleMapsUrls.openInApp(stops, null))
    }
}
