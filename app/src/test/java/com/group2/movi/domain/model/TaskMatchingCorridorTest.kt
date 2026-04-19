package com.group2.movi.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class TaskMatchingCorridorTest {

    // Reference points around HK/SZ
    private val shenzhenBayOffice = GeoPoint(22.5158, 113.9347)      // carrier's SZ origin
    private val centralHK = GeoPoint(22.2810, 114.1580)               // carrier's HK destination
    private val huaqiangbei = GeoPoint(22.5430, 114.0850)             // near Futian, on corridor
    private val sheungWan = GeoPoint(22.2870, 114.1500)               // near Central, on corridor
    private val kwunTong = GeoPoint(22.3120, 114.2260)                // far off corridor
    private val yuenLong = GeoPoint(22.4450, 114.0350)                // far off corridor

    private val futianCommute = CommuteEntry(
        dayOfWeek = "MONDAY",
        departureTime = "18:00",
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
        // originLocation / destinationLocation are null — legacy data.
    )

    /** 2026-04-20 is a Monday. Build the instant in the test runner's local zone so nextDeparture resolves to the same day. */
    private fun mondayAtLocal(hour: Int, minute: Int = 0): Long =
        LocalDate.of(2026, 4, 20)
            .atTime(hour, minute)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    private fun task(pickup: GeoPoint, dropoff: GeoPoint, now: Long, hoursAhead: Long = 4): Task =
        Task(
            taskId = "t1",
            requesterId = "requester",
            pickupLocation = pickup,
            pickupAddress = "",
            dropoffLocation = dropoff,
            dropoffAddress = "",
            crossingPort = CrossingPort.FUTIAN,
            direction = "SZ_TO_HK",
            requiredBefore = Timestamp(Date(now + hoursAhead * 3_600_000L))
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
        // Segment runs roughly north, point is due east ~0.02 longitude degrees (~2km at this latitude).
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
        val now = mondayAtLocal(16)
        val t = task(pickup = huaqiangbei, dropoff = sheungWan, now = now)
        assertNull(corridorDeviationKm(t, legacyCommute))
    }

    @Test
    fun `corridorDeviationKm is small for tasks on the corridor`() {
        val now = mondayAtLocal(16)
        val t = task(pickup = huaqiangbei, dropoff = sheungWan, now = now)
        val dev = corridorDeviationKm(t, futianCommute)
        assertNotNull(dev)
        assertTrue("expected < 5km, got $dev", dev!! < 5.0)
    }

    @Test
    fun `corridorDeviationKm is large for tasks off the corridor`() {
        val now = mondayAtLocal(16)
        val t = task(pickup = kwunTong, dropoff = yuenLong, now = now)
        val dev = corridorDeviationKm(t, futianCommute)
        assertNotNull(dev)
        assertTrue("expected >> 5km, got $dev", dev!! > 8.0)
    }

    @Test
    fun `findBestMatch prefers corridor commute and reports corridor reason`() {
        // now = Monday 16:00 local → departure 18:00 local (same day) → deadline 20:00 local → 120 min before deadline.
        val now = mondayAtLocal(16)
        val t = task(pickup = huaqiangbei, dropoff = sheungWan, now = now, hoursAhead = 4)

        val match = findBestMatch(t, listOf(futianCommute), now)
        assertNotNull("corridor task should match", match)
        assertTrue(match!!.corridorMatch)
        assertTrue(
            "reason should mention corridor: ${match.reason}",
            match.reason.contains("commute corridor")
        )
    }

    @Test
    fun `findBestMatch filters out off-corridor tasks when commute has a corridor`() {
        val now = mondayAtLocal(16)
        val t = task(pickup = kwunTong, dropoff = yuenLong, now = now, hoursAhead = 4)

        val match = findBestMatch(t, listOf(futianCommute), now)
        assertNull("off-corridor task should be filtered", match)
    }

    @Test
    fun `legacy commute without corridor falls back to port-center detour and still matches`() {
        val now = mondayAtLocal(16)
        val t = task(pickup = huaqiangbei, dropoff = sheungWan, now = now, hoursAhead = 4)

        val match = findBestMatch(t, listOf(legacyCommute), now)
        assertNotNull("legacy schedule should still match via fallback", match)
        assertFalse("fallback should not claim corridor match", match!!.corridorMatch)
        assertTrue(
            "reason should mention port not corridor: ${match.reason}",
            match.reason.contains("Via ")
        )
    }
}
