package com.group2.movi.domain.model

import com.google.firebase.firestore.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskMatchingCorridorTest {

    private val shenzhenBayOffice = GeoPoint(22.5158, 113.9347)
    private val centralHK = GeoPoint(22.2810, 114.1580)
    private val nearCorridorStart = GeoPoint(22.5000, 113.9500)
    private val nearCorridorEnd = GeoPoint(22.2950, 114.1650)
    private val farNorthEast = GeoPoint(22.6500, 114.3000)
    private val farNorthWest = GeoPoint(22.6400, 113.7800)

    private val futianCommute = CommuteEntry(
        daysOfWeek = listOf("MONDAY"),
        direction = "SZ_TO_HK",
        originLocation = shenzhenBayOffice,
        originAddress = "Shenzhen Bay Office",
        destinationLocation = centralHK,
        destinationAddress = "Central"
    )

    private val legacyCommute = CommuteEntry(
        dayOfWeek = "MONDAY",
        departureTime = "18:00",
        port = CrossingPort.FUTIAN,
        direction = "SZ_TO_HK"
    )

    private fun task(
        pickup: GeoPoint,
        dropoff: GeoPoint,
        crossingPort: String = "",
        direction: String = "SZ_TO_HK"
    ): Task = Task(
        taskId = "t1",
        requesterId = "requester",
        pickupLocation = pickup,
        pickupAddress = "",
        dropoffLocation = dropoff,
        dropoffAddress = "",
        crossingPort = crossingPort,
        direction = direction
    )

    @Test
    fun `pointToSegmentKm returns zero when point equals segment endpoint`() {
        val a = GeoPoint(22.5, 114.0)
        val b = GeoPoint(22.6, 114.1)
        assertEquals(0.0, pointToSegmentKm(a, a, b), 0.01)
        assertEquals(0.0, pointToSegmentKm(b, a, b), 0.01)
    }

    @Test
    fun `pointToSegmentKm projects to the nearest interior point`() {
        val a = GeoPoint(22.50, 114.00)
        val b = GeoPoint(22.60, 114.00)
        val p = GeoPoint(22.55, 114.02)
        val dist = pointToSegmentKm(p, a, b)
        assertTrue("expected ~2km, got $dist", dist in 1.8..2.2)
    }

    @Test
    fun `corridor is empty when commute has no origin or destination`() {
        assertTrue(commuteCorridor(legacyCommute).isEmpty())
    }

    @Test
    fun `corridor yields two points when commute is complete`() {
        val corridor = commuteCorridor(futianCommute)
        assertEquals(2, corridor.size)
        assertEquals(shenzhenBayOffice, corridor[0])
        assertEquals(centralHK, corridor[1])
    }

    @Test
    fun `corridorDeviationKm returns null for legacy commutes`() {
        val t = task(pickup = shenzhenBayOffice, dropoff = centralHK)
        assertNull(corridorDeviationKm(t, legacyCommute))
    }

    @Test
    fun `corridorDeviationKm is small for tasks on the corridor`() {
        val t = task(pickup = shenzhenBayOffice, dropoff = centralHK)
        val dev = corridorDeviationKm(t, futianCommute)
        assertNotNull(dev)
        assertTrue("expected < 1km, got $dev", dev!! < 1.0)
    }

    @Test
    fun `routeOverlapPercent is low for far routes`() {
        val t = task(pickup = farNorthEast, dropoff = farNorthWest)
        val percent = routeOverlapPercent(t, futianCommute)
        assertNotNull(percent)
        assertTrue("expected below threshold, got $percent", percent!! in 0 until HIGH_ROUTE_MATCH_PERCENT)
    }

    @Test
    fun `matchPercentForDeviationKm uses the MVP breakpoint curve`() {
        assertEquals(100, matchPercentForDeviationKm(0.0))
        assertEquals(100, matchPercentForDeviationKm(3.0))
        assertEquals(90, matchPercentForDeviationKm(5.0))
        assertEquals(75, matchPercentForDeviationKm(8.0))
        assertEquals(60, matchPercentForDeviationKm(12.0))
        assertEquals(0, matchPercentForDeviationKm(20.0))
        assertEquals(0, matchPercentForDeviationKm(24.0))
    }

    @Test
    fun `findBestMatch returns high route match percent for corridor task`() {
        val t = task(pickup = shenzhenBayOffice, dropoff = centralHK)

        val match = findBestMatch(t, listOf(futianCommute))
        assertNotNull("corridor task should match", match)
        assertTrue(match!!.corridorMatch)
        assertTrue(match.matchPercent >= HIGH_ROUTE_MATCH_PERCENT)
        assertTrue(match.reason == "Right on your route" || match.reason.contains("off your route"))
        assertEquals(0.0, match.routeOffsetKm, 0.1)
        assertEquals("Mon", match.scheduleSummary)
    }

    @Test
    fun `findBestMatch can still match near routes across a different port`() {
        val t = task(
            pickup = nearCorridorStart,
            dropoff = nearCorridorEnd,
            crossingPort = CrossingPort.LO_WU
        )

        val match = findBestMatch(t, listOf(futianCommute))
        assertNotNull(match)
        assertTrue(match!!.matchPercent > 0)
    }

    @Test
    fun `findBestMatch rejects opposite direction`() {
        val t = task(
            pickup = shenzhenBayOffice,
            dropoff = centralHK,
            direction = "HK_TO_SZ"
        )

        assertNull(findBestMatch(t, listOf(futianCommute)))
    }

    @Test
    fun `legacy commute without corridor does not produce route overlap`() {
        val t = task(pickup = shenzhenBayOffice, dropoff = centralHK)

        val match = findBestMatch(t, listOf(legacyCommute))
        assertNull(match)
        assertFalse(commuteCorridor(legacyCommute).isNotEmpty())
    }
}
