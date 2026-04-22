package com.group2.movi.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapsConfigTest {

    @Test
    fun `configured api key returns null for placeholders`() {
        assertNull(MapsConfig.configuredApiKey(""))
        assertNull(MapsConfig.configuredApiKey("DISABLED"))
        assertNull(MapsConfig.configuredApiKey("YOUR_MAPS_API_KEY"))
        assertNull(MapsConfig.configuredApiKey("YOUR_SHARED_MAPS_API_KEY"))
    }

    @Test
    fun `configured api key returns trimmed real key`() {
        assertEquals("AIza-real-key", MapsConfig.configuredApiKey(" AIza-real-key "))
    }
}
