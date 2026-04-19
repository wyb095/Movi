package com.group2.movi.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.storage.FirebaseStorage
import com.group2.movi.domain.model.CommuteEntry
import com.group2.movi.domain.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import android.net.Uri

@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage
) {
    private fun users() = firestore.collection("users")

    val currentUid: String? get() = auth.currentUser?.uid

    suspend fun getUser(uid: String): User? {
        return try {
            users().document(uid).get().await().toObject<User>()
        } catch (e: Exception) {
            null
        }
    }

    fun observeUser(uid: String): Flow<User?> = callbackFlow {
        val reg = users().document(uid).addSnapshotListener { snap, error ->
            if (error != null) {
                trySend(null)
                return@addSnapshotListener
            }
            trySend(runCatching { snap?.toObject<User>() }.getOrNull())
        }
        awaitClose { reg.remove() }
    }

    fun observeUsersWithCommuteSchedule(excludeUid: String? = null): Flow<List<User>> = callbackFlow {
        val reg = users().addSnapshotListener { snap, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snap?.documents
                ?.mapNotNull { doc -> runCatching { doc.toObject<User>() }.getOrNull() }
                ?.filter { it.commuteSchedule.isNotEmpty() && it.userId != excludeUid }
                ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    suspend fun updateDisplayName(uid: String, name: String): Result<Unit> = runCatching {
        users().document(uid).update("displayName", name).await()
        Unit
    }

    suspend fun updateFcmToken(uid: String, token: String): Result<Unit> = runCatching {
        users().document(uid).update("fcmToken", token).await()
        Unit
    }

    suspend fun uploadProfilePhoto(uid: String, uri: Uri): Result<String> = runCatching {
        val ref = storage.reference.child("profile_photos/$uid.jpg")
        ref.putFile(uri).await()
        val url = ref.downloadUrl.await().toString()
        users().document(uid).update("profilePhotoUrl", url).await()
        url
    }

    suspend fun saveCommuteSchedule(uid: String, schedule: List<CommuteEntry>): Result<Unit> =
        runCatching {
            users().document(uid).update("commuteSchedule", schedule).await()
            Unit
        }

    suspend fun addCommuteEntry(uid: String, entry: CommuteEntry): Result<Unit> = runCatching {
        users().document(uid).update("commuteSchedule", FieldValue.arrayUnion(entry)).await()
        Unit
    }

    suspend fun removeCommuteEntry(uid: String, entry: CommuteEntry): Result<Unit> = runCatching {
        val userRef = users().document(uid)
        firestore.runTransaction { tx ->
            val currentSchedule = tx.get(userRef).toObject<User>()?.commuteSchedule.orEmpty()
            tx.update(userRef, "commuteSchedule", filterOutCommuteEntry(currentSchedule, entry))
        }.await()
        Unit
    }
}

internal fun filterOutCommuteEntry(
    schedule: List<CommuteEntry>,
    entry: CommuteEntry
): List<CommuteEntry> = schedule.filterNot { it == entry }
