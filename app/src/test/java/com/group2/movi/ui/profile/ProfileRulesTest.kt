package com.group2.movi.ui.profile

import com.google.firebase.Timestamp
import com.group2.movi.domain.model.WEEKEND_DAYS
import com.group2.movi.domain.model.WORKDAY_DAYS
import com.group2.movi.domain.model.normalizeDaysOfWeek
import com.group2.movi.domain.model.summarizeDaysOfWeek
import com.group2.movi.domain.model.Task
import com.group2.movi.domain.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class ProfileRulesTest {

    @Test
    fun `earnings count only confirmed tasks`() {
        assertTrue(countsTowardsEarnings(Task(status = TaskStatus.CONFIRMED)))
        assertFalse(countsTowardsEarnings(Task(status = TaskStatus.DELIVERED)))
        assertFalse(countsTowardsEarnings(Task(status = TaskStatus.ACCEPTED)))
    }

    @Test
    fun `days of week are normalized and summarized`() {
        assertEquals(
            WORKDAY_DAYS,
            normalizeDaysOfWeek(listOf("FRIDAY", "MONDAY", "WEDNESDAY", "TUESDAY", "THURSDAY"))
        )
        assertEquals("Weekdays", summarizeDaysOfWeek(WORKDAY_DAYS))
        assertEquals("Weekend", summarizeDaysOfWeek(WEEKEND_DAYS))
        assertEquals("Mon, Wed, Fri", summarizeDaysOfWeek(listOf("WEDNESDAY", "MONDAY", "FRIDAY")))
    }

    @Test
    fun `schedule day toggles expand and collapse presets`() {
        assertEquals(
            WORKDAY_DAYS,
            togglePresetDays(emptyList(), WORKDAY_DAYS)
        )
        assertEquals(
            emptyList<String>(),
            togglePresetDays(WORKDAY_DAYS, WORKDAY_DAYS)
        )
        assertEquals(
            listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SUNDAY"),
            toggleSpecificDay(WORKDAY_DAYS, "SUNDAY")
        )
    }

    @Test
    fun `earnings are bucketed by escrow release time`() {
        val now = 1_000_000_000_000L
        val releasedRecently = Task(
            status = TaskStatus.CONFIRMED,
            finalPrice = 120.0,
            deliveredAt = Timestamp(Date(now - 40L * DAY_MS)),
            escrowReleasedAt = Timestamp(Date(now - DAY_MS))
        )
        val releasedLongAgo = Task(
            status = TaskStatus.CONFIRMED,
            finalPrice = 80.0,
            deliveredAt = Timestamp(Date(now - DAY_MS)),
            escrowReleasedAt = Timestamp(Date(now - 45L * DAY_MS))
        )

        val (week, month, all) = calculateEarnings(listOf(releasedRecently, releasedLongAgo), now)

        assertEquals(120.0, week, 0.0)
        assertEquals(120.0, month, 0.0)
        assertEquals(200.0, all, 0.0)
    }

    @Test
    fun `earnings fall back to delivered time when release time is missing`() {
        val now = 1_000_000_000_000L
        val task = Task(
            status = TaskStatus.CONFIRMED,
            offeredPrice = 66.0,
            deliveredAt = Timestamp(Date(now - 2L * DAY_MS))
        )

        val (week, month, all) = calculateEarnings(listOf(task), now)

        assertEquals(now - 2L * DAY_MS, earningTimestampMillis(task))
        assertEquals(66.0, week, 0.0)
        assertEquals(66.0, month, 0.0)
        assertEquals(66.0, all, 0.0)
    }

    private companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
