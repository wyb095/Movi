package com.group2.movi.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import com.group2.movi.domain.model.InteractionAction
import com.group2.movi.domain.model.InteractionContext
import com.group2.movi.domain.model.PreferenceProfile
import com.group2.movi.domain.model.UserInteraction
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFERENCE_LOOKBACK_DAYS = 90L

@Singleton
class UserPreferenceRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val interactions = firestore.collection("user_interactions")

    fun observePreferenceProfile(userId: String): Flow<PreferenceProfile> = callbackFlow {
        val reg = interactions
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(200)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trySend(PreferenceProfile.empty(userId))
                    return@addSnapshotListener
                }
                val list = snap?.documents
                    ?.mapNotNull { doc -> runCatching { doc.toObject<UserInteraction>() }.getOrNull() }
                    ?: emptyList()
                trySend(analyzePreferences(userId, list))
            }
        awaitClose { reg.remove() }
    }

    suspend fun recordTaskInteraction(
        userId: String,
        taskId: String,
        action: InteractionAction,
        context: InteractionContext
    ) {
        val payload = hashMapOf(
            "userId" to userId,
            "taskId" to taskId,
            "action" to action.name,
            "taskCategory" to context.category,
            "taskPrice" to context.price,
            "matchPercent" to context.matchPercent,
            "timestamp" to Timestamp.now()
        )
        interactions.add(payload).await()
    }

    private fun analyzePreferences(
        userId: String,
        rawInteractions: List<UserInteraction>
    ): PreferenceProfile {
        val since = System.currentTimeMillis() - PREFERENCE_LOOKBACK_DAYS * 24L * 60 * 60 * 1000
        val recent = rawInteractions.filter { interaction ->
            (interaction.timestamp?.toDate()?.time ?: 0L) >= since
        }
        val strongestByTask = recent
            .groupBy { it.taskId }
            .values
            .map { events ->
                events.maxByOrNull { strengthFor(it.parsedAction) } ?: events.first()
            }
        val positive = strongestByTask.filter {
            it.parsedAction == InteractionAction.ACCEPT || it.parsedAction == InteractionAction.COMPLETE
        }
        val sortedCategories = positive
            .groupingBy { it.taskCategory }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .map { it.first }
            .take(2)
        val medianPrice = positive
            .map { it.taskPrice }
            .filter { it > 0.0 }
            .sorted()
            .let(::medianOrNull)
        return PreferenceProfile(
            userId = userId,
            topCategories = sortedCategories,
            medianAcceptedPriceHkd = medianPrice,
            sampleSize = positive.size,
            lastUpdated = Timestamp(Date())
        )
    }

    private fun medianOrNull(values: List<Double>): Double? {
        if (values.isEmpty()) return null
        val mid = values.size / 2
        return if (values.size % 2 == 1) {
            values[mid]
        } else {
            (values[mid - 1] + values[mid]) / 2.0
        }
    }

    private fun strengthFor(action: InteractionAction): Int = when (action) {
        InteractionAction.VIEW -> 1
        InteractionAction.ACCEPT -> 2
        InteractionAction.COMPLETE -> 3
    }
}
