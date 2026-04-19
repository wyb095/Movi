package com.group2.movi.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.group2.movi.domain.model.Review
import com.group2.movi.domain.model.ReviewRole
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun reviews() = firestore.collection("reviews")
    private fun users() = firestore.collection("users")
    private fun tasks() = firestore.collection("tasks")

    suspend fun postReview(review: Review): Result<Unit> = runCatching {
        val reviewRef = reviews().document("${review.taskId}_${review.reviewerId}")
        val userRef = users().document(review.revieweeId)
        val taskRef = tasks().document(review.taskId)
        firestore.runTransaction { tx ->
            // Firestore transactions require ALL reads before ANY writes.
            val reviewSnap = tx.get(reviewRef)
            if (reviewSnap.exists()) {
                throw IllegalStateException("You already reviewed this task.")
            }
            val userSnap = tx.get(userRef)
            val currentRating = userSnap.getDouble("rating") ?: 0.0
            val currentCount = userSnap.getLong("totalReviews") ?: 0L
            val newCount = currentCount + 1
            val newAvg = (currentRating * currentCount + review.rating) / newCount

            val toWrite = review.copy(createdAt = Timestamp.now())
            tx.set(reviewRef, toWrite)
            tx.update(userRef, mapOf(
                "rating" to newAvg,
                "totalReviews" to newCount
            ))

            val flagField = if (review.reviewerRole == ReviewRole.REQUESTER) {
                "requesterReviewed"
            } else {
                "carrierReviewed"
            }
            tx.update(taskRef, flagField, true)
        }.await()
        Unit
    }

    fun observeReviewsForUser(uid: String): Flow<List<Review>> = callbackFlow {
        val reg = reviews()
            .whereEqualTo("revieweeId", uid)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents
                    ?.mapNotNull { doc -> runCatching { doc.toObject<Review>() }.getOrNull() }
                    ?.sortedByDescending { it.createdAt?.seconds ?: 0L }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }
}
