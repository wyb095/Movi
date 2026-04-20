package com.group2.movi.data.repository

import android.util.Log
import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.storage.FirebaseStorage
import com.group2.movi.domain.model.EscrowStatus
import com.group2.movi.domain.model.Task
import com.group2.movi.domain.model.TaskStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.random.Random
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private fun tasks() = firestore.collection("tasks")

    /** Observes open tasks, optionally filtered by category. */
    fun observeOpenTasks(category: String? = null): Flow<List<Task>> = callbackFlow {
        var query: Query = tasks().whereEqualTo("status", TaskStatus.OPEN)
        if (category != null) query = query.whereEqualTo("category", category)

        val reg = query.limit(50).addSnapshotListener { snap, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snap?.documents
                ?.mapNotNull { doc -> runCatching { doc.toObject<Task>() }.getOrNull() }
                ?.sortedByDescending { it.createdAt?.seconds ?: 0L }
                ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    /** Tasks the user posted as a requester. */
    fun observeTasksAsRequester(uid: String): Flow<List<Task>> = callbackFlow {
        val reg = tasks()
            .whereEqualTo("requesterId", uid)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents
                    ?.mapNotNull { doc -> runCatching { doc.toObject<Task>() }.getOrNull() }
                    ?.sortedByDescending { it.createdAt?.seconds ?: 0L }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    /** Tasks the user accepted as a carrier. */
    fun observeTasksAsCarrier(uid: String): Flow<List<Task>> = callbackFlow {
        val reg = tasks()
            .whereEqualTo("carrierId", uid)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents
                    ?.mapNotNull { doc -> runCatching { doc.toObject<Task>() }.getOrNull() }
                    ?.sortedByDescending { it.createdAt?.seconds ?: 0L }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeTask(taskId: String): Flow<Task?> = callbackFlow {
        val reg = tasks().document(taskId).addSnapshotListener { snap, error ->
            if (error != null) {
                trySend(null)
                return@addSnapshotListener
            }
            trySend(runCatching { snap?.toObject<Task>() }.getOrNull())
        }
        awaitClose { reg.remove() }
    }

    suspend fun getTask(taskId: String): Result<Task> = runCatching {
        val snap = tasks().document(taskId).get().await()
        snap.toObject<Task>() ?: throw IllegalStateException("Task not found.")
    }

    suspend fun uploadItemPhoto(uri: Uri): Result<String> = runCatching {
        val ref = storage.reference.child("task_photos/${UUID.randomUUID()}.jpg")
        ref.putFile(uri).await()
        ref.downloadUrl.await().toString()
    }

    suspend fun postTask(task: Task): Result<String> = runCatching {
        val toWrite = task.copy(
            status = TaskStatus.OPEN,
            createdAt = Timestamp.now()
        )
        val ref = tasks().add(toWrite).await()
        Log.i(
            "TaskRepository",
            "Posted task docId=${ref.id}, requesterId=${task.requesterId}, title=${task.title}"
        )
        ref.id
    }.onFailure { error ->
        Log.e(
            "TaskRepository",
            "Failed to post task for requesterId=${task.requesterId}, title=${task.title}",
            error
        )
    }

    suspend fun acceptTaskAtomic(taskId: String, carrierId: String, carrierName: String): Result<Unit> =
        runCatching {
            val ref = tasks().document(taskId)
            firestore.runTransaction { tx ->
                val snap = tx.get(ref)
                val status = snap.getString("status") ?: TaskStatus.OPEN
                if (status != TaskStatus.OPEN) {
                    throw IllegalStateException("This task has already been accepted.")
                }
                val requesterId = snap.getString("requesterId")
                if (requesterId == carrierId) {
                    throw IllegalStateException("You cannot accept your own task.")
                }
                val offeredPrice = snap.getDouble("offeredPrice") ?: 0.0
                val pin = generateDeliveryPin()
                tx.update(
                    ref,
                    mapOf(
                        "carrierId" to carrierId,
                        "carrierName" to carrierName,
                        "status" to TaskStatus.ACCEPTED,
                        "acceptedAt" to Timestamp.now(),
                        "finalPrice" to offeredPrice,
                        "escrowHeld" to true,
                        "escrowStatus" to EscrowStatus.HELD,
                        "escrowHoldAmount" to offeredPrice,
                        "escrowHeldAt" to Timestamp.now(),
                        "deliveryPin" to pin,
                        "deliveryQrToken" to buildQrToken(taskId, pin)
                    )
                )
            }.await()
            Unit
        }

    suspend fun markPickedUp(taskId: String): Result<Unit> = runCatching {
        val ref = tasks().document(taskId)
        firestore.runTransaction { tx ->
            val snap = tx.get(ref)
            val status = snap.getString("status") ?: TaskStatus.OPEN
            if (status != TaskStatus.ACCEPTED) {
                throw IllegalStateException("Task must be accepted before pickup.")
            }
            tx.update(
                ref,
                mapOf(
                    "status" to TaskStatus.PICKED_UP,
                    "pickedUpAt" to Timestamp.now()
                )
            )
        }.await()
        Unit
    }

    suspend fun markDelivered(taskId: String): Result<Unit> = runCatching {
        val ref = tasks().document(taskId)
        firestore.runTransaction { tx ->
            val snap = tx.get(ref)
            val status = snap.getString("status") ?: TaskStatus.OPEN
            if (status != TaskStatus.PICKED_UP) {
                throw IllegalStateException("Task must be picked up before delivery.")
            }
            tx.update(
                ref,
                mapOf(
                    "status" to TaskStatus.DELIVERED,
                    "deliveredAt" to Timestamp.now()
                )
            )
        }.await()
        Unit
    }

    suspend fun confirmReceipt(taskId: String, verificationCode: String): Result<Unit> = runCatching {
        val taskRef = tasks().document(taskId)
        val users = firestore.collection("users")
        firestore.runTransaction { tx ->
            // Firestore transactions require ALL reads before ANY writes.
            val snap = tx.get(taskRef)
            val status = snap.getString("status") ?: TaskStatus.OPEN
            val escrowStatus = snap.getString("escrowStatus") ?: EscrowStatus.NOT_HELD
            if (status == TaskStatus.CONFIRMED || escrowStatus == EscrowStatus.RELEASED) {
                throw IllegalStateException("Task is already confirmed.")
            }
            if (!canConfirmReceipt(status, escrowStatus)) {
                throw IllegalStateException("Task is not ready for confirmation.")
            }

            val pin = snap.getString("deliveryPin").orEmpty()
            val qrToken = snap.getString("deliveryQrToken").orEmpty()
            val code = verificationCode.trim()
            if (code.isBlank() || (code != pin && code != qrToken)) {
                throw IllegalStateException("Invalid PIN or QR token.")
            }

            val carrierId = snap.getString("carrierId").orEmpty()
            val amount = snap.getDouble("finalPrice")
                ?: snap.getDouble("offeredPrice")
                ?: 0.0

            val carrierRef = if (carrierId.isNotBlank()) users.document(carrierId) else null
            val carrierSnap = carrierRef?.let { tx.get(it) }
            val currentEarnings = carrierSnap?.getDouble("totalEarnings") ?: 0.0
            val completed = carrierSnap?.getLong("tasksCompleted") ?: 0L

            tx.update(
                taskRef,
                mapOf(
                    "status" to TaskStatus.CONFIRMED,
                    "escrowHeld" to false,
                    "escrowStatus" to EscrowStatus.RELEASED,
                    "escrowReleasedAt" to Timestamp.now()
                )
            )

            if (carrierRef != null) {
                tx.update(
                    carrierRef,
                    mapOf(
                        "totalEarnings" to currentEarnings + amount,
                        "tasksCompleted" to completed + 1
                    )
                )
            }
        }.await()
        Unit
    }

    suspend fun cancelTask(taskId: String): Result<Unit> = runCatching {
        val ref = tasks().document(taskId)
        firestore.runTransaction { tx ->
            val snap = tx.get(ref)
            val status = snap.getString("status") ?: TaskStatus.OPEN
            if (status != TaskStatus.OPEN) {
                throw IllegalStateException("Only open tasks can be cancelled.")
            }
            tx.update(
                ref,
                mapOf(
                    "status" to TaskStatus.CANCELLED,
                    "escrowHeld" to false,
                    "escrowStatus" to EscrowStatus.NOT_HELD
                )
            )
        }.await()
        Unit
    }

    suspend fun raiseDispute(taskId: String): Result<Unit> = runCatching {
        tasks().document(taskId).update(
            mapOf(
                "status" to TaskStatus.DISPUTED,
                "escrowStatus" to EscrowStatus.DISPUTED
            )
        ).await()
        Unit
    }

    private fun generateDeliveryPin(): String =
        Random.nextInt(100000, 999999).toString()

    private fun buildQrToken(taskId: String, pin: String): String =
        "MOVI-${taskId.takeLast(6).uppercase()}-$pin"
}

internal fun canConfirmReceipt(status: String, escrowStatus: String): Boolean {
    return status == TaskStatus.DELIVERED && escrowStatus == EscrowStatus.HELD
}
