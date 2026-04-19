package com.group2.movi.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPermissionRulesTest {

    @Test
    fun `notification permission is not required before android 13`() {
        assertTrue(canPostNotifications(sdkInt = 32, permissionGranted = false))
    }

    @Test
    fun `notification permission is required on android 13 and above`() {
        assertFalse(canPostNotifications(sdkInt = 33, permissionGranted = false))
        assertTrue(canPostNotifications(sdkInt = 33, permissionGranted = true))
    }
}
