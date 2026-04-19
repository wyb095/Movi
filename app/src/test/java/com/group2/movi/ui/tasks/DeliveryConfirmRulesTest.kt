package com.group2.movi.ui.tasks

import com.group2.movi.domain.model.Task
import com.group2.movi.domain.model.TaskStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryConfirmRulesTest {

    private val requesterId = "requester"
    private val carrierId = "carrier"

    @Test
    fun `only requester or carrier can review after delivery`() {
        val task = Task(
            requesterId = requesterId,
            carrierId = carrierId,
            status = TaskStatus.DELIVERED
        )

        assertTrue(canReviewTask(task, requesterId))
        assertTrue(canReviewTask(task, carrierId))
        assertFalse(canReviewTask(task, "outsider"))
    }

    @Test
    fun `review is blocked before delivery`() {
        val task = Task(
            requesterId = requesterId,
            carrierId = carrierId,
            status = TaskStatus.ACCEPTED
        )

        assertFalse(canReviewTask(task, requesterId))
        assertFalse(canReviewTask(task, carrierId))
    }
}
