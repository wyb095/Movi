package com.group2.movi.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val profilePhotoUrl: String? = null,
    val role: List<String> = listOf("requester", "carrier"), // users can be both
    val isVerified: Boolean = false,
    val rating: Double = 0.0,
    val totalReviews: Int = 0,
    val totalEarnings: Double = 0.0,
    val tasksCompleted: Int = 0,
    val fcmToken: String? = null,
    val createdAt: Timestamp? = null,
    val commuteSchedule: List<CommuteEntry> = emptyList()
)

data class CommuteEntry(
    val dayOfWeek: String = "", // MONDAY, TUESDAY, ...
    val departureTime: String = "", // "18:00"
    val port: String = "", // FUTIAN, LO_WU, HUANGGANG, LOK_MA_CHAU, HEUNG_YUEN_WAI
    val direction: String = "" // HK_TO_SZ or SZ_TO_HK
)
