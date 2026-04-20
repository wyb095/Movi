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
    private val huaqiangbei = GeoPoint(22.5430, 114.0850)
    private val sheungWan = GeoPoint(22.2870, 114.1500)
    private val kwunTong = GeoPoint(22.3120, 114.2260)
    private val yuenLong = GeoPoint(22.4450, 114.0350)

    private val futianCommute = CommuteEntry(
        daysOfWeek = listOf("MONDAY"),
        port = CrossingPort.FUTIAN,
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
        port: String = CrossingPort.FUTIAN,
        direction: String = "SZ_TO_HK"
    ): Task = Task(
        taskId = "t1",
        requesterId = "requester",
        pickupLocation = pickup,
        pickupAddress = "",
        dropoffLocation = dropoff,
        dropoffAddress = "",
        crossingPort = port,
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
    fun `corridor yields three points when commute is complete`() {
        val corridor = commuteCorridor(futianCommute)
        assertEquals(3, corridor.size)
        assertEquals(shenzhenBayOffice, corridor[0])
        assertEquals(centralHK, corridor[2])
    }

    @Test
    fun `corridorDeviationKm returns null for legacy commutes`() {
        val t = task(pickup = huaqiangbei, dropoff = sheungWan)
        assertNull(corridorDeviationKm(t, legacyCommute))
    }

    @Test
    fun `corridorDeviationKm is small for tasks on the corridor`() {
        val t = task(pickup = huaqiangbei, dropoff = sheungWan)
        val dev = corridorDeviationKm(t, futianCommute)
        assertNotNull(dev)
        assertTrue("expected < 5km, got $dev", dev!! < 5.0)
    }

    @Test
    fun `routeOverlapPercent is low for far routes`() {
        val t = task(pickup = kwunTong, dropoff = yuenLong)
        val percent = routeOverlapPercent(t, futianCommute)
        assertNotNull(percent)
        assertTrue("expected below threshold, got $percent", percent!! in 1 until HIGH_ROUTE_MATCH_PERCENT)
    }

    @Test
    fun `findBestMatch returns high route match percent for corridor task`() {
        val t = task(pickup = huaqiangbei, dropoff = sheungWan)

        val match = findBestMatch(t, listOf(futianCommute))
        assertNotNull("corridor task should match", match)
        assertTrue(match!!.corridorMatch)
        assertTrue(match.matchPercent >= HIGH_ROUTE_MATCH_PERCENT)
        assertTrue(match.reason.contains("route match"))
        assertEquals("Mon", match.scheduleSummary)
    }

    @Test
    fun `findBestMatch can still match near routes across a different port`() {
        val t = task(
            pickup = huaqiangbei,
            dropoff = sheungWan,
            port = CrossingPort.LO_WU
        )

        val match = findBestMatch(t, listOf(futianCommute))
        assertNotNull(match)
        assertTrue(match!!.matchPercent > 0)
    }

    @Test
    fun `findBestMatch rejects opposite direction`() {
        val t = task(
            pickup = huaqiangbei,
            dropoff = sheungWan,
            direction = "HK_TO_SZ"
        )

        assertNull(findBestMatch(t, listOf(futianCommute)))
    }

    @Test
    fun `legacy commute without corridor does not produce route overlap`() {
        val t = task(pickup = huaqiangbei, dropoff = sheungWan)

        val match = findBestMatch(t, listOf(legacyCommute))
        assertNull(match)
        assertFalse(commuteCorridor(legacyCommute).isNotEmpty())
    }
}
