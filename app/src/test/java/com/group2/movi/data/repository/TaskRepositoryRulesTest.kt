package com.group2.movi.data.repository

import com.group2.movi.domain.model.EscrowStatus
import com.group2.movi.domain.model.TaskStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskRepositoryRulesTest {

    @Test
    fun `confirmation is allowed only when task is delivered and escrow is held`() {
        assertTrue(canConfirmReceipt(TaskStatus.DELIVERED, EscrowStatus.HELD))
        assertFalse(canConfirmReceipt(TaskStatus.CONFIRMED, EscrowStatus.RELEASED))
        assertFalse(canConfirmReceipt(TaskStatus.DELIVERED, EscrowStatus.RELEASED))
        assertFalse(canConfirmReceipt(TaskStatus.PICKED_UP, EscrowStatus.HELD))
    }
}
