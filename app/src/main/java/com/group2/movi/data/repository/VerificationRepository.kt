package com.group2.movi.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.group2.movi.domain.model.RealNameVerification
import com.group2.movi.domain.model.calculateTrustScore
import com.group2.movi.domain.model.trustBadgeFor
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VerificationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions
) {
    suspend fun verifyRealName(
        userId: String,
        realName: String,
        idCardNumber: String
    ): Result<RealNameVerification> = runCatching {
        val result = functions
            .getHttpsCallable("verifyRealName")
            .call(
                mapOf(
                    "realName" to realName.trim(),
                    "idCardNumber" to idCardNumber.trim()
                )
            )
            .await()
        val data = result.data as? Map<*, *> ?: emptyMap<String, Any>()
        val verification = RealNameVerification(
            realName = (data["realName"] as? String).orEmpty().ifBlank { realName.trim() },
            idCardNumberHash = (data["idCardNumberHash"] as? String).orEmpty(),
            idCardLast4 = (data["idCardLast4"] as? String).orEmpty().ifBlank { idCardNumber.takeLast(4) },
            verifiedAt = Timestamp.now(),
            verificationProvider = (data["verificationProvider"] as? String).orEmpty().ifBlank { "cloud_function" }
        )
        val userRef = firestore.collection("users").document(userId)
        firestore.runTransaction { tx ->
            val snap = tx.get(userRef)
            val score = calculateTrustScore(
                isEmailVerified = snap.getBoolean("isVerified") == true,
                hasRealNameVerification = true,
                rating = snap.getDouble("rating") ?: 0.0,
                totalReviews = (snap.getLong("totalReviews") ?: 0L).toInt(),
                tasksCompleted = (snap.getLong("tasksCompleted") ?: 0L).toInt()
            )
            tx.update(
                userRef,
                mapOf(
                    "realNameVerification" to verification,
                    "trustScore" to score,
                    "trustBadge" to trustBadgeFor(score).name
                )
            )
        }.await()
        verification
    }
}
