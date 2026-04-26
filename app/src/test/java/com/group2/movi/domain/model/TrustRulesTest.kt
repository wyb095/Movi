package com.group2.movi.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrustRulesTest {

    @Test
    fun `trust score uses the full weighted formula`() {
        val score = calculateTrustScore(
            isEmailVerified = true,
            hasRealNameVerification = true,
            rating = 5.0,
            totalReviews = 12,
            tasksCompleted = 20
        )

        assertEquals(100, score)
        assertEquals(TrustBadge.PLATINUM, trustBadgeFor(score))
    }

    @Test
    fun `cold start users stay at the default trust score`() {
        val user = User(displayName = "New carrier")

        assertEquals(DEFAULT_TRUST_SCORE, trustScoreFor(user))
        assertEquals(TrustBadge.BRONZE, trustBadgeFor(trustScoreFor(user)))
        assertTrue(user.realNameVerification == null)
    }
}
