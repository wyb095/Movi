package com.group2.movi.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComplianceCheckerTest {

    @Test
    fun `prohibited keywords block posting`() {
        val alerts = ComplianceChecker.checkTaskCompliance(
            Task(
                category = TaskCategory.PARCEL,
                title = "Need help carrying a gun case",
                description = "Cross-border drop-off"
            )
        )

        assertEquals(1, alerts.size)
        assertEquals(ComplianceSeverity.BLOCKING, alerts.first().severity)
        assertEquals(ComplianceAlertType.PROHIBITED_ITEM, alerts.first().type)
    }

    @Test
    fun `medicine tasks warn about documents`() {
        val alerts = ComplianceChecker.checkTaskCompliance(
            Task(
                category = TaskCategory.MEDICINE,
                title = "Cold medicine",
                description = "Need it tonight",
                offeredPrice = 80.0,
                declaredItemValueHkd = 1600.0
            )
        )

        assertTrue(alerts.any { it.type == ComplianceAlertType.DOCUMENTATION_REQUIRED })
        assertTrue(alerts.any { it.type == ComplianceAlertType.VALUE_WARNING })
    }
}
