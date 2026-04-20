package com.group2.movi.ui.home

import com.group2.movi.ui.components.displayablePlace
import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayablePlaceTest {

    @Test
    fun `skips leading Google Plus Code segment`() {
        assertEquals(
            "Clear Water Bay",
            displayablePlace("JRP7+PRQ, Clear Water Bay, Hong Kong", "fallback")
        )
    }

    @Test
    fun `lowercase plus code is also skipped`() {
        assertEquals(
            "Clear Water Bay",
            displayablePlace("jrp7+prq, Clear Water Bay, Hong Kong", "fallback")
        )
    }

    @Test
    fun `keeps first readable segment when it is not a plus code`() {
        assertEquals(
            "Clear Water Bay",
            displayablePlace("Clear Water Bay Rd, Hong Kong", "fallback")
        )
    }

    @Test
    fun `road-like leading segment falls back to district or landmark`() {
        assertEquals(
            "Nanshan",
            displayablePlace(
                "3688 Nan Hai Da Dao, Nan Shan Qu, Shen Zhen Shi, Guang Dong Sheng, China, 518060",
                "fallback"
            )
        )
    }

    @Test
    fun `returns fallback when address is blank`() {
        assertEquals("fallback", displayablePlace("", "fallback"))
        assertEquals("fallback", displayablePlace("   ", "fallback"))
    }

    @Test
    fun `all plus code segments return fallback`() {
        assertEquals(
            "fallback",
            displayablePlace("JRP7+PRQ", "fallback")
        )
    }

    @Test
    fun `truncates segment to 30 chars`() {
        val long = "Some Very Long Street Name That Exceeds Thirty Characters, City"
        val out = displayablePlace(long, "fallback")
        assertEquals(30, out.length)
        assertEquals("Some Very Long Street Name Tha", out)
    }

    @Test
    fun `plain country name passes through`() {
        assertEquals("China", displayablePlace("China", "fallback"))
        assertEquals("Hong Kong", displayablePlace("Hong Kong", "fallback"))
    }

    @Test
    fun `generic region pair keeps a stable fallback label`() {
        assertEquals("China", displayablePlace("China, Hong Kong", "fallback"))
    }

    @Test
    fun `trims whitespace around segments`() {
        assertEquals(
            "Shenzhen Bay",
            displayablePlace("   Shenzhen Bay ,  Nanshan District  ,China", "fallback")
        )
    }
}
