package com.group2.movi.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Message(
    @DocumentId val messageId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val messageType: String = MessageType.TEXT,
    val imageUrl: String? = null,
    val imageLabel: String? = null,
    val sentAt: Timestamp? = null,
    val isRead: Boolean = false
)

object MessageType {
    const val TEXT = "TEXT"
    const val IMAGE = "IMAGE"
}

data class Review(
    @DocumentId val reviewId: String = "",
    val taskId: String = "",
    val reviewerId: String = "",
    val reviewerName: String = "",
    val revieweeId: String = "",
    val reviewerRole: String = ReviewRole.REQUESTER,
    val revieweeRole: String = ReviewRole.CARRIER,
    val rating: Int = 5,
    val comment: String = "",
    val createdAt: Timestamp? = null
)

object ReviewRole {
    const val REQUESTER = "REQUESTER"
    const val CARRIER = "CARRIER"
}
