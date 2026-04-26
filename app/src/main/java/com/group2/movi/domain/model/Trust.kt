package com.group2.movi.domain.model

import com.google.firebase.Timestamp
import kotlin.math.roundToInt

const val DEFAULT_TRUST_SCORE = 40

data class RealNameVerification(
    val realName: String = "",
    val idCardNumberHash: String = "",
    val idCardLast4: String = "",
    val verifiedAt: Timestamp? = null,
    val verificationProvider: String = ""
)

enum class TrustBadge {
    BRONZE,
    SILVER,
    GOLD,
    PLATINUM
}

fun trustBadgeFor(score: Int): TrustBadge = when (score.coerceIn(0, 100)) {
    in 0..59 -> TrustBadge.BRONZE
    in 60..74 -> TrustBadge.SILVER
    in 75..89 -> TrustBadge.GOLD
    else -> TrustBadge.PLATINUM
}

fun calculateTrustScore(
    isEmailVerified: Boolean,
    hasRealNameVerification: Boolean,
    rating: Double,
    totalReviews: Int,
    tasksCompleted: Int
): Int {
    val ratingPoints = ((rating.coerceIn(0.0, 5.0) / 5.0) * 20.0).roundToInt()
    val reviewPoints = totalReviews.coerceIn(0, 10)
    val taskPoints = tasksCompleted.coerceIn(0, 15)
    val score = DEFAULT_TRUST_SCORE +
        (if (isEmailVerified) 5 else 0) +
        (if (hasRealNameVerification) 10 else 0) +
        ratingPoints +
        reviewPoints +
        taskPoints
    return score.coerceIn(0, 100)
}

fun trustScoreFor(user: User): Int = calculateTrustScore(
    isEmailVerified = user.isVerified,
    hasRealNameVerification = user.realNameVerification != null,
    rating = user.rating,
    totalReviews = user.totalReviews,
    tasksCompleted = user.tasksCompleted
)
