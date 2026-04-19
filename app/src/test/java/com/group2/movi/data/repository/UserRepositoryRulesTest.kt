package com.group2.movi.data.repository

import com.group2.movi.domain.model.CommuteEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class UserRepositoryRulesTest {

    @Test
    fun `filterOutCommuteEntry removes matching legacy-shaped schedule entry`() {
        val legacyEntry = CommuteEntry(
            dayOfWeek = "MONDAY",
            departureTime = "08:30",
            port = "FUTIAN",
            direction = "SZ_TO_HK"
        )
        val newerEntry = CommuteEntry(
            dayOfWeek = "TUESDAY",
            departureTime = "18:00",
            port = "LO_WU",
            direction = "HK_TO_SZ",
            originAddress = "Central",
            destinationAddress = "Futian"
        )

        val filtered = filterOutCommuteEntry(listOf(legacyEntry, newerEntry), legacyEntry)

        assertEquals(listOf(newerEntry), filtered)
    }
}
