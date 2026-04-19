package com.group2.movi.data.repository

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.storage.FirebaseStorage
import com.group2.movi.domain.model.Message
import com.group2.movi.domain.model.MessageType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private fun messages(taskId: String) =
        firestore.collection("tasks").document(taskId).collection("messages")

    suspend fun uploadChatImage(uri: Uri): Result<String> = runCatching {
        val ref = storage.reference.child("chat_images/${UUID.randomUUID()}.jpg")
        ref.putFile(uri).await()
        ref.downloadUrl.await().toString()
    }

    fun observeMessages(taskId: String): Flow<List<Message>> = callbackFlow {
        val reg = messages(taskId).addSnapshotListener { snap, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snap?.documents
                ?.mapNotNull { doc -> runCatching { doc.toObject<Message>() }.getOrNull() }
                ?.sortedBy { it.sentAt?.seconds ?: 0L }
                ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    suspend fun sendMessage(
        taskId: String,
        senderId: String,
        senderName: String,
        text: String,
        imageUrl: String? = null,
        imageLabel: String? = null
    ): Result<Unit> =
        runCatching {
            val msg = Message(
                senderId = senderId,
                senderName = senderName,
                text = text,
                messageType = if (imageUrl.isNullOrBlank()) MessageType.TEXT else MessageType.IMAGE,
                imageUrl = imageUrl,
                imageLabel = imageLabel,
                sentAt = Timestamp.now(),
                isRead = false
            )
            messages(taskId).add(msg).await()
            Unit
        }
}
