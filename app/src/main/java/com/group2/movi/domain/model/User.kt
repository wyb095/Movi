package com.group2.movi.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint

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
    val realNameVerification: RealNameVerification? = null,
    val trustScore: Int = DEFAULT_TRUST_SCORE,
    val trustBadge: TrustBadge = TrustBadge.BRONZE,
    val fcmToken: String? = null,
    val createdAt: Timestamp? = null,
    val commuteSchedule: List<CommuteEntry> = emptyList()
)

data class CommuteEntry(
    val daysOfWeek: List<String> = emptyList(), // MONDAY..SUNDAY
    val dayOfWeek: String = "", // MONDAY, TUESDAY, ...
    val departureTime: String = "", // "18:00"
    val port: String = "", // legacy Firestore compatibility placeholder
    val direction: String = "", // HK_TO_SZ or SZ_TO_HK
    val originLocation: GeoPoint? = null,
    val originAddress: String = "",
    val destinationLocation: GeoPoint? = null,
    val destinationAddress: String = ""
)

val ORDERED_DAYS_OF_WEEK = listOf(
    "MONDAY",
    "TUESDAY",
    "WEDNESDAY",
    "THURSDAY",
    "FRIDAY",
    "SATURDAY",
    "SUNDAY"
)

val WORKDAY_DAYS = ORDERED_DAYS_OF_WEEK.take(5)
val WEEKEND_DAYS = ORDERED_DAYS_OF_WEEK.takeLast(2)

fun normalizeDaysOfWeek(values: Iterable<String>): List<String> {
    val chosen = values
        .map { it.trim().uppercase() }
        .filter { it in ORDERED_DAYS_OF_WEEK }
        .toSet()
    return ORDERED_DAYS_OF_WEEK.filter(chosen::contains)
}

fun shortDayLabel(day: String): String = when (day) {
    "MONDAY" -> "Mon"
    "TUESDAY" -> "Tue"
    "WEDNESDAY" -> "Wed"
    "THURSDAY" -> "Thu"
    "FRIDAY" -> "Fri"
    "SATURDAY" -> "Sat"
    "SUNDAY" -> "Sun"
    else -> day
}

fun summarizeDaysOfWeek(values: Iterable<String>): String {
    val days = normalizeDaysOfWeek(values)
    if (days.isEmpty()) return "No days selected"
    return when {
        days == WORKDAY_DAYS -> "Weekdays"
        days == WEEKEND_DAYS -> "Weekend"
        else -> days.joinToString(", ") { shortDayLabel(it) }
    }
}

fun CommuteEntry.normalizedDaysOfWeek(): List<String> {
    val newDays = normalizeDaysOfWeek(daysOfWeek)
    return if (newDays.isNotEmpty()) {
        newDays
    } else {
        normalizeDaysOfWeek(listOf(dayOfWeek))
    }
}

fun CommuteEntry.scheduleDaySummary(): String = summarizeDaysOfWeek(normalizedDaysOfWeek())
