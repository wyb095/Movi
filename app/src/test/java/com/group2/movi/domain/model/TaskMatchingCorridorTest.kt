package com.group2.movi.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date

class TaskMatchingCorridorTest {

    private val origin = GeoPoint(22.5405, 114.0590)
    private val destination = GeoPoint(22.2849, 114.1589)
    private val nowMillis = ZonedDateTime.of(
        LocalDate.of(2026, 4, 20),
        LocalTime.of(8, 0),
        ZoneId.systemDefault()
    ).toInstant().toEpochMilli()
    private val deadline = Timestamp(
        Date(
            ZonedDateTime.of(
                LocalDate.of(2026, 4, 20),
                LocalTime.of(12, 0),
                ZoneId.systemDefault()
            ).toInstant().toEpochMilli()
        )
    )

    @Test
    fun `corridor task matches with low detour`() {
        val match = findBestMatch(
            task = task(
                pickup = origin,
                dropoff = destination
            ),
            schedule = listOf(corridorEntry()),
            nowMillis = nowMillis
        )

        assertNotNull(match)
        assertTrue(match!!.detourMinutes < 20)
        assertTrue(match.reason.startsWith("Within your commute corridor"))
    }

    @Test
    fun `outside corridor task is filtered out`() {
        val match = findBestMatch(
            task = task(
                pickup = origin,
                dropoff = GeoPoint(22.3122, 114.2264)
            ),
            schedule = listOf(corridorEntry()),
            nowMillis = nowMillis
        )

        assertNull(match)
    }

    @Test
    fun `legacy schedule falls back to original detour estimate`() {
        val task = task(
            pickup = origin,
            dropoff = destination
        )
        val match = findBestMatch(
            task = task,
            schedule = listOf(
                CommuteEntry(
                    dayOfWeek = "MONDAY",
                    departureTime = "09:00",
                    port = CrossingPort.FUTIAN,
                    direction = "SZ_TO_HK"
                )
            ),
            nowMillis = nowMillis
        )

        assertNotNull(match)
        assertEquals(estimateDetourMinutes(task), match!!.detourMinutes)
        assertTrue(match.reason.startsWith("Near ${CrossingPort.label(CrossingPort.FUTIAN)}"))
    }

    @Test
    fun `pointToSegmentKm handles endpoints extension and perpendicular projection`() {
        val a = GeoPoint(22.0, 114.0)
        val b = GeoPoint(22.0, 114.1)

        assertEquals(0.0, pointToSegmentKm(a, a, b), 1e-6)
        assertEquals(0.0, pointToSegmentKm(b, a, b), 1e-6)
        assertEquals(10.3, pointToSegmentKm(GeoPoint(22.0, 114.2), a, b), 0.4)
        assertEquals(1.11, pointToSegmentKm(GeoPoint(22.01, 114.05), a, b), 0.15)
    }

    private fun corridorEntry(): CommuteEntry = CommuteEntry(
        dayOfWeek = "MONDAY",
        departureTime = "09:00",
        port = CrossingPort.FUTIAN,
        direction = "SZ_TO_HK",
        originLocation = origin,
        originAddress = "Futian CBD",
        destinationLocation = destination,
        destinationAddress = "Central"
    )

    private fun task(pickup: GeoPoint, dropoff: GeoPoint): Task = Task(
        requesterId = "requester",
        category = TaskCategory.PARCEL,
        title = "Deliver documents",
        pickupLocation = pickup,
        pickupAddress = "Pickup",
        dropoffLocation = dropoff,
        dropoffAddress = "Drop-off",
        crossingPort = CrossingPort.FUTIAN,
        direction = "SZ_TO_HK",
        requiredBefore = deadline,
        offeredPrice = 88.0
    )
}
