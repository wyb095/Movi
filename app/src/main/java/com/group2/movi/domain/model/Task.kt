package com.group2.movi.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint

data class Task(
    @DocumentId val taskId: String = "",
    val requesterId: String = "",
    val requesterName: String = "",
    val requesterRating: Double = 0.0,
    val carrierId: String? = null,
    val carrierName: String? = null,
    val status: String = TaskStatus.OPEN, // OPEN, ACCEPTED, PICKED_UP, DELIVERED, CANCELLED, DISPUTED
    val category: String = TaskCategory.PARCEL,
    val title: String = "",
    val description: String = "",
    val itemPhotoUrl: String? = null,
    val pickupLocation: GeoPoint? = null,
    val pickupAddress: String = "",
    val dropoffLocation: GeoPoint? = null,
    val dropoffAddress: String = "",
    val crossingPort: String = "", // legacy Firestore compatibility placeholder
    val direction: String = "SZ_TO_HK", // HK_TO_SZ or SZ_TO_HK
    val requiredBefore: Timestamp? = null,
    val offeredPrice: Double = 0.0,
    val finalPrice: Double? = null,
    val escrowHeld: Boolean = false,
    val escrowStatus: String = EscrowStatus.NOT_HELD,
    val escrowHoldAmount: Double = 0.0,
    val deliveryPin: String = "",
    val deliveryQrToken: String = "",
    val customsDeclaration: Boolean = false,
    val isUrgent: Boolean = false,
    val requesterReviewed: Boolean = false,
    val carrierReviewed: Boolean = false,
    val createdAt: Timestamp? = null,
    val acceptedAt: Timestamp? = null,
    val pickedUpAt: Timestamp? = null,
    val deliveredAt: Timestamp? = null,
    val escrowHeldAt: Timestamp? = null,
    val escrowReleasedAt: Timestamp? = null
)

object TaskStatus {
    const val OPEN = "OPEN"
    const val ACCEPTED = "ACCEPTED"
    const val PICKED_UP = "PICKED_UP"
    const val DELIVERED = "DELIVERED"
    const val CONFIRMED = "CONFIRMED"
    const val CANCELLED = "CANCELLED"
    const val DISPUTED = "DISPUTED"
}

object TaskCategory {
    const val FOOD = "FOOD"
    const val PARCEL = "PARCEL"
    const val DOCUMENT = "DOCUMENT"
    const val MEDICINE = "MEDICINE"
    const val OTHER = "OTHER"
    val ALL = listOf(FOOD, PARCEL, DOCUMENT, MEDICINE, OTHER)
}

object EscrowStatus {
    const val NOT_HELD = "NOT_HELD"
    const val HELD = "HELD"
    const val RELEASED = "RELEASED"
    const val DISPUTED = "DISPUTED"
}

object CrossingPort {
    const val FUTIAN = "FUTIAN"
    const val LO_WU = "LO_WU"
    const val HUANGGANG = "HUANGGANG"
    const val LOK_MA_CHAU = "LOK_MA_CHAU"
    const val HEUNG_YUEN_WAI = "HEUNG_YUEN_WAI"
    val ALL = listOf(FUTIAN, LO_WU, HUANGGANG, LOK_MA_CHAU, HEUNG_YUEN_WAI)

    fun label(port: String): String = when (port) {
        FUTIAN -> "Futian / Lok Ma Chau Spur Line"
        LO_WU -> "Lo Wu"
        HUANGGANG -> "Huanggang"
        LOK_MA_CHAU -> "Lok Ma Chau"
        HEUNG_YUEN_WAI -> "Heung Yuen Wai"
        else -> port
    }
}
