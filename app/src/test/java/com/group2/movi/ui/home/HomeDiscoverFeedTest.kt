package com.group2.movi.ui.home

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.group2.movi.domain.model.CommuteEntry
import com.group2.movi.domain.model.CrossingPort
import com.group2.movi.domain.model.HIGH_ROUTE_MATCH_PERCENT
import com.group2.movi.domain.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class HomeDiscoverFeedTest {

    private val routeCommute = CommuteEntry(
        daysOfWeek = listOf("MONDAY", "WEDNESDAY", "FRIDAY"),
        port = CrossingPort.FUTIAN,
        direction = "SZ_TO_HK",
        originLocation = GeoPoint(22.5158, 113.9347),
        originAddress = "Shenzhen Bay Office",
        destinationLocation = GeoPoint(22.2810, 114.1580),
        destinationAddress = "Central"
    )

    private val legacyCommute = CommuteEntry(
        dayOfWeek = "MONDAY",
        departureTime = "18:00",
        port = CrossingPort.FUTIAN,
        direction = "SZ_TO_HK"
    )

    private val huaqiangbei = GeoPoint(22.5430, 114.0850)
    private val nearCentral = GeoPoint(22.2870, 114.1500)
    private val slightlyOffShenzhen = GeoPoint(22.5280, 114.1020)
    private val slightlyOffHongKong = GeoPoint(22.3000, 114.1820)
    private val kwunTong = GeoPoint(22.3120, 114.2260)
    private val yuenLong = GeoPoint(22.4450, 114.0350)

    private fun task(
        id: String,
        requesterId: String,
        createdAtSeconds: Long,
        port: String = CrossingPort.FUTIAN,
        direction: String = "SZ_TO_HK",
        isUrgent: Boolean = false,
        pickupLocation: GeoPoint? = null,
        dropoffLocation: GeoPoint? = null
    ): Task = Task(
        taskId = id,
        requesterId = requesterId,
        title = id,
        crossingPort = port,
        direction = direction,
        isUrgent = isUrgent,
        pickupLocation = pickupLocation,
        dropoffLocation = dropoffLocation,
        createdAt = Timestamp(Date(createdAtSeconds * 1000))
    )

    @Test
    fun `buildDiscoverFeed ranks high-overlap tasks first then low-overlap then own`() {
        val matchedTop = task(
            id = "matched-top",
            requesterId = "other-a",
            createdAtSeconds = 100,
            pickupLocation = huaqiangbei,
            dropoffLocation = nearCentral
        )
        val matchedLower = task(
            id = "matched-lower",
            requesterId = "other-b",
            createdAtSeconds = 200,
            pickupLocation = slightlyOffShenzhen,
            dropoffLocation = slightlyOffHongKong
        )
        val unmatched = task(
            id = "unmatched",
            requesterId = "other-c",
            createdAtSeconds = 300,
            pickupLocation = kwunTong,
            dropoffLocation = yuenLong
        )
        val own = task(
            id = "own-task",
            requesterId = "me",
            createdAtSeconds = 400,
            isUrgent = true,
            pickupLocation = huaqiangbei,
            dropoffLocation = nearCentral
        )

        val feed = buildDiscoverFeed(
            tasks = listOf(unmatched, own, matchedLower, matchedTop),
            currentUid = "me",
            schedule = listOf(routeCommute)
        )

        assertEquals(
            listOf("matched-top", "matched-lower", "unmatched", "own-task"),
            feed.map { it.task.taskId }
        )
        assertEquals(
            listOf(
                DiscoverMatchState.MATCHED,
                DiscoverMatchState.MATCHED,
                DiscoverMatchState.UNMATCHED,
                DiscoverMatchState.OWN
            ),
            feed.map { it.matchState }
        )
        assertTrue((feed.first().matchPercent ?: 0) >= HIGH_ROUTE_MATCH_PERCENT)
        assertTrue(feed.last().isOwnTask)
    }

    @Test
    fun `buildDiscoverFeed keeps low-overlap tasks visible as unmatched`() {
        val closeTask = task(
            id = "close-task",
            requesterId = "other-a",
            createdAtSeconds = 100,
            pickupLocation = huaqiangbei,
            dropoffLocation = nearCentral
        )
        val lowOverlapTask = task(
            id = "low-overlap",
            requesterId = "other-b",
            createdAtSeconds = 200,
            pickupLocation = kwunTong,
            dropoffLocation = yuenLong
        )

        val feed = buildDiscoverFeed(
            tasks = listOf(lowOverlapTask, closeTask),
            currentUid = "me",
            schedule = listOf(routeCommute)
        )

        assertEquals(listOf("close-task", "low-overlap"), feed.map { it.task.taskId })
        assertEquals(DiscoverMatchState.MATCHED, feed.first().matchState)
        assertEquals(DiscoverMatchState.UNMATCHED, feed.last().matchState)
        assertNotNull(feed.last().matchPercent)
        assertTrue((feed.last().matchPercent ?: 100) < HIGH_ROUTE_MATCH_PERCENT)
    }

    @Test
    fun `buildDiscoverFeed without usable route schedule falls back to market order`() {
        val otherUrgent = task(
            id = "other-urgent",
            requesterId = "other-a",
            createdAtSeconds = 100,
            isUrgent = true,
            pickupLocation = huaqiangbei,
            dropoffLocation = nearCentral
        )
        val otherRecent = task(
            id = "other-recent",
            requesterId = "other-b",
            createdAtSeconds = 200,
            pickupLocation = slightlyOffShenzhen,
            dropoffLocation = slightlyOffHongKong
        )
        val ownUrgent = task(
            id = "own-urgent",
            requesterId = "me",
            createdAtSeconds = 300,
            isUrgent = true,
            pickupLocation = huaqiangbei,
            dropoffLocation = nearCentral
        )

        val feed = buildDiscoverFeed(
            tasks = listOf(otherRecent, ownUrgent, otherUrgent),
            currentUid = "me",
            schedule = listOf(legacyCommute)
        )

        assertEquals(
            listOf("other-urgent", "other-recent", "own-urgent"),
            feed.map { it.task.taskId }
        )
        assertEquals(
            listOf(
                DiscoverMatchState.UNMATCHED,
                DiscoverMatchState.UNMATCHED,
                DiscoverMatchState.OWN
            ),
            feed.map { it.matchState }
        )
        assertNull(feed.first().matchPercent)
    }

    @Test
    fun `buildDiscoverFeed without schedule keeps own tasks behind other open tasks`() {
        val otherUrgent = task(
            id = "other-urgent",
            requesterId = "other-a",
            createdAtSeconds = 100,
            isUrgent = true
        )
        val otherRecent = task(
            id = "other-recent",
            requesterId = "other-b",
            createdAtSeconds = 200
        )
        val ownUrgent = task(
            id = "own-urgent",
            requesterId = "me",
            createdAtSeconds = 300,
            isUrgent = true
        )

        val feed = buildDiscoverFeed(
            tasks = listOf(otherRecent, ownUrgent, otherUrgent),
            currentUid = "me",
            schedule = emptyList()
        )

        assertEquals(
            listOf("other-urgent", "other-recent", "own-urgent"),
            feed.map { it.task.taskId }
        )
        assertNull(feed.first().matchPercent)
        assertEquals(DiscoverMatchState.OWN, feed.last().matchState)
    }
}
